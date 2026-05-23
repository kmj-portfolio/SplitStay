package staysplit.hotel_reservation.payment.mapper;

import org.springframework.stereotype.Component;
import staysplit.hotel_reservation.payment.domain.dto.response.CancelPaymentResponse;
import staysplit.hotel_reservation.payment.domain.dto.response.PaymentResponse;
import staysplit.hotel_reservation.payment.domain.entity.PaymentEntity;
import staysplit.hotel_reservation.reservation.domain.entity.ReservationEntity;
import staysplit.hotel_reservation.payment.domain.dto.response.CreatePaymentResponse;

@Component
public class PaymentMapper {

    public CreatePaymentResponse toCreatePaymentResponse(PaymentEntity payment) {
        return new CreatePaymentResponse(
                payment.getPaymentId(),
                payment.getAmount());
    }

    public PaymentResponse toPaymentResponse(PaymentEntity paymentEntity) {
        ReservationEntity reservation = paymentEntity.getReservationParticipant().getReservation();

        return PaymentResponse.builder()
                .paymentId(paymentEntity.getPaymentId())
                .paymentAmount(paymentEntity.getAmount())
                .status(paymentEntity.getStatus())
                .payMethod(paymentEntity.getPayMethod())
                .cardPublisher(paymentEntity.getCardPublisherName())
                .reservationId(reservation.getId())
                .reservationNumber(reservation.getReservationNumber())
                .paidAt(paymentEntity.getCreatedAt())
                .payMethod(paymentEntity.getPayMethod())
                .build();
    }

    public CancelPaymentResponse toCancelPaymentResponse(PaymentEntity paymentEntity) {

        return new CancelPaymentResponse(
                paymentEntity.getPaymentId(),
                paymentEntity.getStatus().toString(),
                paymentEntity.getAmount());
    }
}
