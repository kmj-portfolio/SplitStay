package staysplit.hotel_reservation.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staysplit.hotel_reservation.customer.domain.entity.CustomerEntity;
import staysplit.hotel_reservation.customer.service.CustomerValidator;
import staysplit.hotel_reservation.payment.domain.dto.request.CreatePaymentRequest;
import staysplit.hotel_reservation.payment.domain.dto.response.CancelPaymentResponse;
import staysplit.hotel_reservation.payment.domain.dto.response.PaymentResponse;
import staysplit.hotel_reservation.payment.domain.entity.PaymentEntity;
import staysplit.hotel_reservation.payment.domain.enums.PaymentStatus;
import staysplit.hotel_reservation.payment.mapper.PaymentMapper;
import staysplit.hotel_reservation.payment.repository.PaymentRepository;
import staysplit.hotel_reservation.reservation.domain.entity.ReservationEntity;
import staysplit.hotel_reservation.reservation.domain.entity.ReservationParticipantEntity;
import staysplit.hotel_reservation.reservation.reposiotry.ReservationParticipantRepository;
import staysplit.hotel_reservation.reservation.service.ReservationValidator;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentTransactionService {

    private final PaymentRepository paymentRepository;
    private final ReservationParticipantRepository participantRepository;
    private final PaymentMapper paymentMapper;
    private final ReservationValidator reservationValidator;
    private final CustomerValidator customerValidator;

    @Transactional
    public PaymentResponse createPayment(String email, CreatePaymentRequest request) {
        CustomerEntity customerEntity = customerValidator.validateCustomerByEmail(email);
        ReservationEntity reservation = reservationValidator.validateReservation(request.reservationId());
        ReservationParticipantEntity reservationParticipant = reservationValidator.validateReservationParticipant(
                customerEntity.getId(), reservation.getId());

        PaymentEntity paymentEntity = PaymentEntity.builder()
                .reservationParticipant(reservationParticipant)
                .paymentId(createPaymentId(reservation.getReservationNumber()))
                .amount(reservationParticipant.getSplitAmount())
                .status(PaymentStatus.READY)
                .build();

        paymentRepository.save(paymentEntity);

        return paymentMapper.toPaymentResponse(paymentEntity);
    }

    @Transactional
    public PaymentResponse processConfirmation(PaymentEntity paymentEntity) {
        // PaymentEntity 상태 변경
        paymentEntity.markPaid();

        // Participant의 결제 상태 변경
        ReservationParticipantEntity participant = paymentEntity.getReservationParticipant();
        participant.markPaid();

        // 누적 결제 금액 업데이트
        ReservationEntity reservation = participant.getReservation();
        reservation.addPricePaid(paymentEntity.getAmount());

        // 결제 성공 시 Reservation Status 변경
        changeReservationStatusIfEveryoneHasPaid(reservation);

        return paymentMapper.toPaymentResponse(paymentEntity);
    }

    public CancelPaymentResponse processCancellation(PaymentEntity paymentEntity) {

        // 결제 상태를 CANCELLED로 변경
        paymentEntity.markCancelled();

        // 참여자 결제 상태를 READY 되돌림
        ReservationParticipantEntity participant = paymentEntity.getReservationParticipant();
        participant.markReady();

        // 누적 결제 금액 차감
        ReservationEntity reservation = participant.getReservation();
        reservation.handleCancelledPayment(paymentEntity.getAmount());

        return paymentMapper.toCancelPaymentResponse(paymentEntity);
    }

    private void changeReservationStatusIfEveryoneHasPaid(ReservationEntity reservation) {
        List<ReservationParticipantEntity> participants = participantRepository.findByReservationId(reservation.getId());

        boolean allCompleted = participants.stream().allMatch(p -> p.getPaymentStatus() == PaymentStatus.PAID);

        if (allCompleted) {
            reservation.markConfirmed();
        }
    }

    private String createPaymentId(String reservationNumber) {
        return UUID.randomUUID().toString();
    }
}
