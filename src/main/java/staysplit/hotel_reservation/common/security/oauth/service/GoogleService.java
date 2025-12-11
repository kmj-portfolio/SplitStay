package staysplit.hotel_reservation.common.security.oauth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import staysplit.hotel_reservation.common.exception.AppException;
import staysplit.hotel_reservation.common.exception.ErrorCode;
import staysplit.hotel_reservation.common.security.oauth.dto.AccessTokenDto;
import staysplit.hotel_reservation.common.security.oauth.dto.GoogleProfileDto;

@Slf4j
@Service
public class GoogleService {
    /*

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;
    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;
    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String redirectUri;
    @Value("${spring.security.oauth2.client.registration.google.scope}")
    private String scope;

    public AccessTokenDto getAccessToken(String code) {
        try {

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("code", code);
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("redirect_uri", redirectUri);
            params.add("scope", scope);
            params.add("grant_type", "authorization_code");


            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<AccessTokenDto> response = restTemplate.postForEntity(
                    "https://oauth2.googleapis.com/token",
                    request,
                    AccessTokenDto.class
            );

            return response.getBody();

        } catch (Exception e) {
            log.error("Failed to get Google access token: {}", e.getMessage());
            if (e instanceof HttpClientErrorException) {
                log.error("Google response body: {}", ((HttpClientErrorException) e).getResponseBodyAsString());
            }
            throw new AppException(ErrorCode.OAUTH_PROVIDER_ERROR, "Failed to get access token from Google");
        }
    }

    public GoogleProfileDto getGoogleProfile(String token) {
        RestClient restClient = RestClient.create();
        ResponseEntity<GoogleProfileDto> response = restClient.get()
                .uri("https://openidconnect.googleapis.com/v1/userinfo")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .toEntity(GoogleProfileDto.class);
        return response.getBody();
    }*/
}
