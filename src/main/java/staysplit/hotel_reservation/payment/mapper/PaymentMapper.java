package staysplit.hotel_reservation.payment.mapper;

import org.springframework.stereotype.Component;
import staysplit.hotel_reservation.payment.domain.dto.response.CancelPaymentResponse;
import staysplit.hotel_reservation.payment.domain.dto.response.PaymentResponse;
import staysplit.hotel_reservation.payment.domain.entity.PaymentEntity;
import staysplit.hotel_reservation.reservation.domain.entity.ReservationEntity;

@Component
public class PaymentMapper {

    public PaymentResponse toPaymentResponse(PaymentEntity paymentEntity) {
        ReservationEntity reservation = paymentEntity.getReservation();

        return PaymentResponse.builder()
                .portOnePaymentId(paymentEntity.getPortOnePaymentId())
                .paymentAmount(paymentEntity.getAmount())
                .status(paymentEntity.getStatus())
                .payMethod(paymentEntity.getPayMethod())
                .cardPublisher(paymentEntity.getCardPublisherName())
                .reservationId(reservation.getId())
                .reservationNumber(reservation.getReservationNumber())
                .paidAt(paymentEntity.getPaidAt())
                .payMethod(paymentEntity.getPayMethod())
                .build();
    }

    public CancelPaymentResponse toCancelPaymentResponse(PaymentEntity paymentEntity) {

        return new CancelPaymentResponse(paymentEntity.getPortOnePaymentId(), paymentEntity.getStatus().toString(), paymentEntity.getAmount());
    }
}
