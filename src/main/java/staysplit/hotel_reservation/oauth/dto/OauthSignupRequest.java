package staysplit.hotel_reservation.oauth.dto;

import java.time.LocalDate;

public record OauthSignupRequest(
        String socialId, // From Google OAUTH (sub)
        String email, // From Google OAUTH
        String name, // From Google OAUTH
        String phoneNumber,
        LocalDate birthdate,
        String nickname
) {
}
