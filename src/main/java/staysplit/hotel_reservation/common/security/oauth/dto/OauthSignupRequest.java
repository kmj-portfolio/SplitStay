package staysplit.hotel_reservation.common.security.oauth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OauthSignupRequest {
    private String socialId; // From Google OAUTH (sub)
    private String email; // From Google OAUTH
    private String name; // From Google OAUTH, 카카오는 사용자 입력
    private String nickname; // 사용자 입력값
    private LocalDate birthdate; // 사용자 입력값

}
