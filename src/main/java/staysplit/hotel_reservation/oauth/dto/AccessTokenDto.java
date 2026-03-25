package staysplit.hotel_reservation.oauth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AccessTokenDto(
        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("expires_in")
        int expiresIn,
        
        @JsonProperty("refresh_token")
        String refreshToken,
        
        @JsonProperty("scope")
        String scope
) {
}
