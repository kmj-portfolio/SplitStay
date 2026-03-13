package staysplit.hotel_reservation.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staysplit.hotel_reservation.common.exception.AppException;
import staysplit.hotel_reservation.common.exception.ErrorCode;
import staysplit.hotel_reservation.customer.domain.entity.CustomerEntity;
import staysplit.hotel_reservation.customer.service.CustomerValidator;
import staysplit.hotel_reservation.payment.domain.dto.request.CancelPaymentRequest;
import staysplit.hotel_reservation.payment.domain.dto.request.CreatePaymentRequest;
import staysplit.hotel_reservation.payment.domain.dto.response.CancelPaymentResponse;
import staysplit.hotel_reservation.payment.domain.dto.response.PaymentResponse;
import staysplit.hotel_reservation.payment.domain.dto.response.PortOnePaymentResponse;
import staysplit.hotel_reservation.payment.domain.entity.PaymentEntity;
import staysplit.hotel_reservation.payment.mapper.PaymentMapper;
import staysplit.hotel_reservation.payment.repository.PaymentRepository;
import staysplit.hotel_reservation.reservation.domain.entity.ReservationEntity;
import staysplit.hotel_reservation.reservation.domain.entity.ReservationParticipantEntity;
import staysplit.hotel_reservation.reservation.domain.enums.PaymentStatus;
import staysplit.hotel_reservation.reservation.domain.enums.ReservationStatus;
import staysplit.hotel_reservation.reservation.reposiotry.ReservationParticipantRepository;
import staysplit.hotel_reservation.reservation.reposiotry.ReservationRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PaymentService {

    private final PortOneClient portOneClient;
    private final PaymentRepository paymentRepository;
    private final ReservationParticipantRepository participantRepository;
    private final ReservationRepository reservationRepository;
    private final CustomerValidator customerValidator;
    private final PaymentMapper paymentMapper;

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsByCustomer(String email, Pageable pageable) {
        CustomerEntity customer = customerValidator.validateCustomerByEmail(email);

        return paymentRepository.findByCustomerId(customer.getId(), pageable)
                .map(p -> paymentMapper.toPaymentResponse(p));
    }

    public PaymentResponse verifyAndCreatePayment(String email, CreatePaymentRequest request) {

        log.info("[결제 검증 및 새성 시작] email={}, reservationId={}, portOnePaymentId={}",
                email, request.reservationId(), request.portOnePaymentId());

        CustomerEntity customer = customerValidator.validateCustomerByEmail(email);
        ReservationParticipantEntity participant = validateReservationParticipant(request, customer);

        // 이미 존재하는 PaymentEntity가 있는지 확인
        checkDuplicatePayment(request.portOnePaymentId());

        // PortOne에서 paymentId로 결제 내역 조회
        log.info("[포트원 결제 내역 조회 - portOnePaymentId: {}]", request.portOnePaymentId());
        PortOnePaymentResponse portOnePaymentResponse = portOneClient.getPayment(request.portOnePaymentId());

        // 결제 유효성 검증
        validatePaymentInfo(portOnePaymentResponse, participant.getSplitAmount());

        // 결제 성공 시 Reservation Status 변경
        ReservationEntity reservation = validateReservation(request.reservationId());
        changeReservationStatus(reservation, participant, portOnePaymentResponse.amount().total());

        // PaymentEntity 생성 및 저장
        PaymentEntity paymentEntity = createAndSavePaymentEntity(request, customer, reservation, portOnePaymentResponse, participant);

        return paymentMapper.toPaymentResponse(paymentEntity);
    }

    public CancelPaymentResponse cancelPayment(String email, CancelPaymentRequest request) {
        CustomerEntity customer = customerValidator.validateCustomerByEmail(email);

        PaymentEntity paymentEntity = paymentRepository.findByPortOnePaymentId(request.portOnePaymentId())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_PAYMENT, ErrorCode.INVALID_PAYMENT.getMessage()));

        // 본인 결제인지 확인
        if (!paymentEntity.getCustomer().getId().equals(customer.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED_CUSTOMER, ErrorCode.UNAUTHORIZED_CUSTOMER.getMessage());
        }

        // 이미 취소된 결제인지 확인
        if (paymentEntity.getStatus() == staysplit.hotel_reservation.payment.domain.enums.PaymentStatus.CANCELLED) {
            throw new AppException(ErrorCode.PAYMENT_ALREADY_CANCELLED, ErrorCode.PAYMENT_ALREADY_CANCELLED.getMessage());
        }

        // PortOne 취소 API 호출
        portOneClient.cancelPayment(request.portOnePaymentId(), request.reason());

        // 결제 상태를 CANCELLED로 변경
        paymentEntity.updateStatus(staysplit.hotel_reservation.payment.domain.enums.PaymentStatus.CANCELLED);

        // 참여자 결제 상태를 WAITING으로 되돌림
        ReservationEntity reservation = paymentEntity.getReservation();
        ReservationParticipantEntity participant = participantRepository.findByCustomerIdAndReservationId(customer.getId(), reservation.getId())
                .orElseThrow(() -> new AppException(ErrorCode.RESERVATION_NOT_FOUND, ErrorCode.RESERVATION_NOT_FOUND.getMessage()));
        participant.updatePaymentStatus(PaymentStatus.WAITING);

        // 누적 결제 금액 차감
        reservation.updatePricePaid(-paymentEntity.getAmount());

        // 예약이 CONFIRMED 상태였다면 WAITING_PAYMENT로 되돌림
        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            reservation.updateStatus(ReservationStatus.WAITING_PAYMENT);
        }

        return paymentMapper.toCancelPaymentResponse(paymentEntity);
    }

    private PaymentEntity createAndSavePaymentEntity(CreatePaymentRequest request, CustomerEntity customer, ReservationEntity reservation, PortOnePaymentResponse portOnePaymentResponse, ReservationParticipantEntity participant) {
        log.info("[PaymentEntity 생성 완료]");
        PaymentEntity paymentEntity = PaymentEntity
                .builder()
                .customer(customer)
                .reservation(reservation)
                .portOnePaymentId(request.portOnePaymentId())
                .payMethod(portOnePaymentResponse.methodType())
                .cardPublisherName(portOnePaymentResponse.cardPublisherName())
                .amount(participant.getSplitAmount())
                .status(staysplit.hotel_reservation.payment.domain.enums.PaymentStatus.PAID)
                .build();

        paymentRepository.save(paymentEntity);
        return paymentEntity;
    }

    private ReservationParticipantEntity validateReservationParticipant(CreatePaymentRequest request, CustomerEntity customer) {
        return participantRepository.findByCustomerIdAndReservationId(customer.getId(), request.reservationId())
                .orElseThrow(() -> {
                    log.warn("[예약 참여자로 존재하지 않음] customerId={}, reservationId={}", customer.getId(), request.reservationId());
                    throw new AppException(ErrorCode.RESERVATION_NOT_FOUND, ErrorCode.RESERVATION_NOT_FOUND.getMessage());
                });
    }

    private void checkDuplicatePayment(String portOnePaymentId) {
        if (paymentRepository.existsByPortOnePaymentId(portOnePaymentId)) {
            log.warn("[중복 결제 요청]");
            throw new AppException(ErrorCode.DUPLICATE_PAYMENT, ErrorCode.DUPLICATE_PAYMENT.getMessage());
        }
    }

    private void validatePaymentInfo(PortOnePaymentResponse portOnePaymentResponse, long requiredAmount) {
        // 결제 상태 검증
        if (!"PAID".equalsIgnoreCase(portOnePaymentResponse.status())) {
            log.warn("[결제 미완료 상태] portOnePaymentId={}, status={}", portOnePaymentResponse.id(), portOnePaymentResponse.status());
            throw new AppException(ErrorCode.PAYMENT_INCOMPLETE, ErrorCode.PAYMENT_INCOMPLETE.getMessage());
        }

        // 결제 금액 검증
        log.info("[금액 검증: 내야 하는 금액 {}, 포트원에서 결제한 금액 {}]", requiredAmount, portOnePaymentResponse.amount());
        if (portOnePaymentResponse.amount().total() != requiredAmount) {
            throw new AppException(ErrorCode.PAYMENT_AMOUNT_MISMATCH, ErrorCode.PAYMENT_AMOUNT_MISMATCH.getMessage());
        }
    }

    private void changeReservationStatus(ReservationEntity reservation, ReservationParticipantEntity participant, Long amount) {

        // 만료한 예약이라면 에러 반환
        if (reservation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.EXPIRED_RESERVATION, ErrorCode.EXPIRED_RESERVATION.getMessage());
        }

        // 사용자의 결제 상태를 COMPLETE로 변경
        participant.updatePaymentStatus(PaymentStatus.COMPLETE);

        // 누적 결제 금액 업데이트
        reservation.updatePricePaid(amount);

        // 예약에 대한 참여자 모두의 Payment Status가 COMPLETE면 예약 상태를 CONFIRMED로 변경
        List<ReservationParticipantEntity> participants = participantRepository.findByReservationId(reservation.getId());

        boolean allCompleted = participants.stream().allMatch(p -> p.getPaymentStatus() == PaymentStatus.COMPLETE);

        if (allCompleted) {
            reservation.updateStatus(ReservationStatus.CONFIRMED);
        }
    }

    private ReservationEntity validateReservation(Integer reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new AppException(ErrorCode.RESERVATION_NOT_FOUND, ErrorCode.RESERVATION_NOT_FOUND.getMessage()));
    }
}