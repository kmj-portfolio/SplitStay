package staysplit.hotel_reservation.auth.dto.response;

public record LoginTokens(
        String accessToken,
        String refreshToken,
        String role
) {
}
