package staysplit.hotel_reservation.payment.portone.exception;

import staysplit.hotel_reservation.common.exception.ErrorCode;

public class PortOneException extends RuntimeException {

    private final ErrorCode errorCode;

    public PortOneException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public PortOneException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}
