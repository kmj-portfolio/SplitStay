package staysplit.hotel_reservation.auth.dto.response;

public record UserLoginStatusResponse(
        String email,
        String role,
        Boolean loggedIn
) {
}
