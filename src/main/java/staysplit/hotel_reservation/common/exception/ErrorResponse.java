package staysplit.hotel_reservation.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private String errorCode;
    private String message;

    public static ErrorResponse from(AppException e) {
        return new ErrorResponse(
                e.getErrorCode().name(),
                e.getMessage()
        );
    }
}
