package staysplit.hotel_reservation.oauth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleProfileDto(
        String sub,
        String email,
        String name
) {
}
