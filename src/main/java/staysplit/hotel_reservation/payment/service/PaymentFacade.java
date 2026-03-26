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
import staysplit.hotel_reservation.payment.domain.dto.request.VerifyPaymentRequest;
import staysplit.hotel_reservation.payment.domain.dto.response.CancelPaymentResponse;
import staysplit.hotel_reservation.payment.domain.dto.response.PaymentResponse;
import staysplit.hotel_reservation.payment.domain.dto.response.PortOnePaymentResponse;
import staysplit.hotel_reservation.payment.domain.entity.PaymentEntity;
import staysplit.hotel_reservation.payment.mapper.PaymentMapper;
import staysplit.hotel_reservation.payment.repository.PaymentRepository;
import staysplit.hotel_reservation.payment.webhook.dto.PortOneWebhookRequest;
import staysplit.hotel_reservation.reservation.domain.entity.ReservationEntity;
import staysplit.hotel_reservation.reservation.service.ReservationValidator;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentFacade {

    private final PortOneClient portOneClient;
    private final PaymentTransactionService paymentTransactionService;
    private final PortOnePaymentValidator portOnePaymentValidator;
    private final CustomerValidator customerValidator;
    private final PaymentCompensationService paymentCompensationService;
    private final PaymentRepository paymentRepository;
    private final PaymentValidator paymentValidator;
    private final ReservationValidator reservationValidator;
    private final PaymentMapper paymentMapper;

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsByCustomer(String email, Pageable pageable) {
        CustomerEntity customer = customerValidator.validateCustomerByEmail(email);

        return paymentRepository.findByCustomerId(customer.getId(), pageable)
                .map(paymentMapper::toPaymentResponse);
    }

    public PaymentResponse verifyPayment(String email, VerifyPaymentRequest request) {
        String paymentId = request.paymentId();
        log.info("[결제 검증 및 생성 시작] email={}, portOnePaymentId={}", email, paymentId);

        CustomerEntity customer = customerValidator.validateCustomerByEmail(email);
        PaymentEntity paymentEntity = paymentValidator.validatePaymentByPaymentId(paymentId);

        // 본인 결제인지 확인
        log.debug("[본인 결제인지 확인] customerId={}, paymentId={}", customer.getId(), paymentId);
        paymentValidator.validatePaymentOwner(paymentEntity, customer);

        // 이미 존재하는 PaymentEntity가 있으면 에러 반환 대신 PaymentResponse 반환 (멱등성)
        // TODO: ReservationParticipantId에 대한 PaymentEntitiy가 이미 있고 상태가 WAITING이거나 COMPLETE이면
        //  같은 예약에 대해 중복 결제이기 때문에, 이것을 확인해야 함
        if (paymentEntity.isPaid()) {
            log.info("[결제 중복 요청 - 멱등성 처리] paymentId={}", paymentId);
            return paymentMapper.toPaymentResponse(paymentEntity);
        }

        if (paymentEntity.isCancelled()) {
            log.warn("[결제 검증 실패 - 이미 취소된 결제] paymentId={}", paymentId);
            throw new AppException(ErrorCode.PAYMENT_ALREADY_CANCELLED);
        }

        // 예약이 만료되었으면 fail first
        ReservationEntity reservation = paymentEntity.getReservationParticipant().getReservation();
        log.debug("[예약이 만료되었는지 확인] reservationId={})", reservation.getId());
        reservationValidator.validateNotExpired(reservation);

        // PortOne에서 paymentId로 결제 내역 조회
        log.info("[포트원 결제 내역 조회] portOnePaymentId={}", paymentId);
        PortOnePaymentResponse portOnePaymentResponse = portOneClient.getPayment(paymentId);

        // 요청한 사용자가 결제의 주인이므로, 정상적인 요청이라고 판단한다. 이후로 발생하는 모든 예외에는 포트원 결제를 자동 취소
        // 유효성 검증 후 DB 저장 시도
        try {
            // 결제 유효성 검증
            portOnePaymentValidator.validateForConfirmation(portOnePaymentResponse, paymentEntity);

            // Transaction내에서 PaymentEntity 생성 및 저장
            return paymentTransactionService.processConfirmation(paymentEntity);

        } catch (Exception e) {
            log.error("[결제 처리 중 에러 발생 - 자동 취소 진행] paymentId={}", paymentId, e);
            paymentCompensationService.cancelPaymentSilently(paymentId, "처리 실패로 자동 취소");

            // 결제 취소가 성공하거나 실패해도 여기서 error를 반환
            if (e instanceof AppException) {
                throw (AppException) e;
            }
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
        }
    }

    public CancelPaymentResponse cancelPayment(String email, CancelPaymentRequest request) {
        String paymentId = request.paymentId();
        CustomerEntity customer = customerValidator.validateCustomerByEmail(email);
        PaymentEntity paymentEntity = paymentValidator.validatePaymentByPaymentId(paymentId);

        paymentValidator.validatePaymentOwner(paymentEntity, customer);
        paymentValidator.validateCancelable(paymentEntity);

        portOneClient.cancelPayment(paymentId, request.reason());

        try {
           return paymentTransactionService.processCancellation(paymentEntity);
        } catch (Exception e) {
            log.error("[중요] 포트원 취소는 성공했지만 DB 반영 실패. 수동/재처리 필요. portOnePaymentId={}", paymentId, e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "결제 취소는 완료되었지만 내부 상태 반영에 실패했습니다.");
        }
    }
}