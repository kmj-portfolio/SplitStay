package staysplit.hotel_reservation.auth.dto.request;


public record LoginRequest(
        String email,
        String password
) {
}
