package staysplit.hotel_reservation.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import staysplit.hotel_reservation.common.entity.Response;
import staysplit.hotel_reservation.payment.portone.exception.PortOneException;

@RestControllerAdvice
public class ExceptionManager {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<?> appExceptionHandler(AppException e) {
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(Response.error(ErrorResponse.from(e)));
    }

    @ExceptionHandler(PortOneException.class)
    public ResponseEntity<?> portOneExceptionHandler(PortOneException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> runtimeExceptionHandler(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(e.getMessage());
    }
}