package staysplit.hotel_reservation.common.security.oauth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import staysplit.hotel_reservation.common.security.oauth.dto.AccessTokenDto;
import staysplit.hotel_reservation.common.security.oauth.dto.KakaoProfileDto;

@Service
@RequiredArgsConstructor
public class KakaoService {
/*
    @Value("${oauth.kakao.client-id}")
    private String kakoClientId;

    @Value("${oauth.kakao.redirect-uri}")
    private String kakoRedirectUri;

    public AccessTokenDto getAccessToken(String code) {
        RestClient restClient = RestClient.create();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", kakoClientId);
        params.add("redirect_uri", kakoRedirectUri);
        params.add("grant_type", "authorization_code");

        ResponseEntity<AccessTokenDto> response = restClient.post()
                .uri("https://kauth.kakao.com/oauth/token") // json에 있음
                .header("Content-Type", "application/x-form-urlencoded")
                .body(params)
                .retrieve()
                .toEntity(AccessTokenDto.class);
        return response.getBody();
    }

    public KakaoProfileDto getKakaoProfile(String token) {
        RestClient restClient = RestClient.create();
        ResponseEntity<KakaoProfileDto> response = restClient.get()
                .uri("https://kapi.kakao.com/v2/user/me")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .toEntity(KakaoProfileDto.class);
        return response.getBody();
    }*/
}
