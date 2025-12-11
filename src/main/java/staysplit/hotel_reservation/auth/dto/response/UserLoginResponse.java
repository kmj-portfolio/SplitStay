package staysplit.hotel_reservation.auth.dto.response;

public record UserLoginResponse(
        String accessToken,
        String refreshToken,
        String role
) {
}
