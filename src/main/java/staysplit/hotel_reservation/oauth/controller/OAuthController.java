package staysplit.hotel_reservation.oauth.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;
import staysplit.hotel_reservation.auth.dto.response.LoginResponse;
import staysplit.hotel_reservation.auth.dto.response.LoginTokens;
import staysplit.hotel_reservation.common.entity.Response;
import staysplit.hotel_reservation.common.security.jwt.JwtTokenProvider;
import staysplit.hotel_reservation.oauth.dto.*;
import staysplit.hotel_reservation.oauth.service.GoogleService;
import staysplit.hotel_reservation.oauth.service.OAuthService;
import staysplit.hotel_reservation.user.domain.entity.UserEntity;
import staysplit.hotel_reservation.user.repository.UserRepository;

import java.time.Duration;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/oauth")
public class OAuthController {

    @Value("${jwt.cookie.secure}")
    boolean secureCookie;

    @Value("${jwt.refresh-expiration-days}")
    private int refreshExpirationDays;

    private final OAuthService oAuthService;
    private final GoogleService googleService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/signup")
    public Response<LoginResponse> oauthSignup(@RequestBody OauthSignupRequest request, HttpServletResponse httpServletResponse) {
        LoginTokens response = oAuthService.oauthSignup(request);
        setRefreshTokenCookie(httpServletResponse, response.refreshToken());
        LoginResponse loginResponse = new LoginResponse(response.accessToken(), response.role());
        return Response.success(loginResponse);
    }

    @PostMapping("/google/login")
    public Response<?> googleLogin(@RequestBody RedirectDto redirectDto, HttpServletResponse httpServletResponse) {
        // access token 발급
        AccessTokenDto accessTokenDto = googleService.getAccessToken(redirectDto.code());

        // 사용자 정보 얻기
        GoogleProfileDto googleProfile = googleService.getGoogleProfile(accessTokenDto.accessToken());

        // 회원 가입이 되어 있지 않다면 회원 가입
        Optional<UserEntity> userEntity = userRepository.findBySocialId(googleProfile.sub());

        if (userEntity.isPresent()) {
            // 기존 사용자라면 jwt 반환
            UserEntity user = userEntity.get();
            LoginTokens loginTokens = oAuthService.login(user.getEmail());
            setRefreshTokenCookie(httpServletResponse, loginTokens.refreshToken());
            return Response.success(
                    GoogleLoginResponse.loggedIn(loginTokens.accessToken(), loginTokens.role())
            );
        }
        return Response.success(
                GoogleLoginResponse.signupRequired(googleProfile.sub(), googleProfile.email(), googleProfile.name())
        );
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(secureCookie) // local에서는 false, 프로덱션에서는 true
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofDays(refreshExpirationDays))
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }
}
