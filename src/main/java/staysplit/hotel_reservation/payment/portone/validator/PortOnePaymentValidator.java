package staysplit.hotel_reservation.payment.portone.validator;

import org.springframework.stereotype.Service;
import staysplit.hotel_reservation.common.exception.AppException;
import staysplit.hotel_reservation.common.exception.ErrorCode;
import staysplit.hotel_reservation.payment.portone.dto.response.PortOnePaymentResponse;
import staysplit.hotel_reservation.payment.domain.entity.PaymentEntity;

@Service
public class PortOnePaymentValidator {

    public void validateForConfirmation(PortOnePaymentResponse portOnePaymentResponse,
                                        PaymentEntity paymentEntity) {
        validatePaidStatus(portOnePaymentResponse);
        validateAmount(portOnePaymentResponse, paymentEntity.getAmount());
    }

    private void validatePaidStatus(PortOnePaymentResponse portOnePaymentResponse) {
        if (!"PAID".equalsIgnoreCase(portOnePaymentResponse.status().toString())) {
            throw new AppException(ErrorCode.PAYMENT_INCOMPLETE);
        }
    }

    private void validateAmount(PortOnePaymentResponse portOnePaymentResponse, long requiredAmount) {
        if (portOnePaymentResponse.amount().total() != requiredAmount) {
            throw new AppException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
    }

}


