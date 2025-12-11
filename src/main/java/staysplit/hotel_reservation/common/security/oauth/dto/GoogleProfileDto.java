package staysplit.hotel_reservation.common.security.oauth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleProfileDto {
    private String sub;
    private String email;
    private String given_name;
    private String family_name;
    //private String picture;
}
