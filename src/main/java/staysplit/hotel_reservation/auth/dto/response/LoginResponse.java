package staysplit.hotel_reservation.auth.dto.response;

// Http Response로 나갈 DTO
public record LoginResponse(
        String accessToken,
        String role
){}
