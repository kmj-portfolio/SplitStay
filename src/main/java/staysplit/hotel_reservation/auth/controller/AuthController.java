package staysplit.hotel_reservation.auth.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import staysplit.hotel_reservation.auth.dto.response.AccessTokenResponse;
import staysplit.hotel_reservation.auth.dto.response.LoginResponse;
import staysplit.hotel_reservation.auth.service.AuthService;
import staysplit.hotel_reservation.common.entity.Response;
import staysplit.hotel_reservation.common.exception.AppException;
import staysplit.hotel_reservation.common.exception.ErrorCode;
import staysplit.hotel_reservation.common.security.jwt.JwtTokenProvider;
import staysplit.hotel_reservation.auth.dto.request.LoginRequest;
import staysplit.hotel_reservation.auth.dto.request.PasswordUpdateRequest;
import staysplit.hotel_reservation.auth.dto.response.LoginTokens;
import staysplit.hotel_reservation.auth.dto.response.UserLoginStatusResponse;

import java.time.Duration;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    @Value("${jwt.access-expiration-minutes}")
    private int accessTokenExpiryInMinutes;

    @Value("${jwt.refresh-expiration-days}")
    private int refreshTokenExpiryInDays;

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    // 일반 로그인, 소셜 로그인 아님
    // role을 반환
    @PostMapping("/login")
    public Response<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletResponse httpServletResponse) {

        LoginTokens response = authService.login(loginRequest);

        // create tokens
        String accessToken = response.accessToken();
        String refreshToken = response.refreshToken();
        String role = response.role();

        // Refresh Token을 HttpOnly + Secure + SameSite=Strict 쿠키에 저장
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                        .httpOnly(true)
                        .secure(true)
                        .sameSite("Strict")
                        .path("/")
                        .maxAge(Duration.ofDays(refreshTokenExpiryInDays))
                        .build();

        httpServletResponse.addHeader("Set-Cookie", cookie.toString());

        // access token은 response body에
        return Response.success(new LoginResponse(accessToken, "ROLE_" + role));
    }

    // 비밀 번호 변경
    @PutMapping("/pw")
    public Response<String> changePassword(@RequestBody PasswordUpdateRequest request,
                                           Authentication authentication) {
        String response = authService.changePassword(request, authentication.getName());
        return Response.success(response);
    }

    @PostMapping("/logout")
    public Response<String> logout(@CookieValue(value = "refreshToken", required = false) String refreshToken,
                                   HttpServletResponse httpServletResponse) {
        // Redis에서 refresh Token 삭제
        if (refreshToken != null) {
            jwtTokenProvider.invalidateRefreshToken(refreshToken);
        }

        // 쿠키 삭제 (max age = 0)
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();
        httpServletResponse.addHeader("Set-Cookie", cookie.toString());

        return Response.success("로그아웃 성공");
    }


    @GetMapping("/status")
    public Response<UserLoginStatusResponse> getUserLoginStatus(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new AppException(ErrorCode.USER_NOT_LOGGED_IN);
        }
        String email = userDetails.getUsername();
        String role = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("UNKNOWN");

        UserLoginStatusResponse response = new UserLoginStatusResponse(email, role, true);
        return Response.success(response);
    }

    // access token 재발급
    @PostMapping("/refresh")
    public Response<AccessTokenResponse> refresh(@CookieValue("refreshToken") String refreshToken,
                                         HttpServletResponse response) {

        JwtTokenProvider.TokenPair tokenPair = jwtTokenProvider.recreateAccessTokenAndRotateRefreshToken(refreshToken);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", tokenPair.refreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofDays(refreshTokenExpiryInDays))
                .build();

        response.addHeader("Set-Cookie", cookie.toString());

        return Response.success(new AccessTokenResponse(tokenPair.accessToken()));
    }
}
