package staysplit.hotel_reservation.common.security.oauth.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import staysplit.hotel_reservation.common.entity.Response;
import staysplit.hotel_reservation.common.exception.AppException;
import staysplit.hotel_reservation.common.exception.ErrorCode;
import staysplit.hotel_reservation.common.security.oauth.dto.OauthSignupRequest;
import staysplit.hotel_reservation.common.security.oauth.dto.RedirectDto;
import staysplit.hotel_reservation.common.security.oauth.service.OAuthService;
import staysplit.hotel_reservation.customer.domain.dto.response.CustomerDetailsResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/oauth")
public class OAuthController {

    private final OAuthService oAuthService;
    /*
    @PostMapping("/google/login")
    public Response<?> googleLogin(@RequestBody RedirectDto redirectDto) {
        try {
            // 기존 사용자라면 jwt 반환
            String jwt = oAuthService.getGoogleUserInfo(redirectDto);
            return Response.success(jwt);
        } catch (AppException e) {
            // 신규 사용자라면 추가 정보 입력 필요
            if (e.getErrorCode() == ErrorCode.ADDITIONAL_INFO_REQUIRED) {
                return Response.error(ErrorCode.ADDITIONAL_INFO_REQUIRED);
            }
            throw e; //다른 에러는 그대로 전파
        }
    }

    @PostMapping("/kakao/login")
    public Response<?> kakaoLogin(@RequestBody RedirectDto redirectDto) {
        try {
            // 기존 사용자라면 jwt 반환
            String jwt = oAuthService.kakaoLogin(redirectDto);
            return Response.success(jwt);
        } catch (AppException e) {
            // 신규 사용자라면 추가 정보 입력 필요
            if (e.getErrorCode() == ErrorCode.ADDITIONAL_INFO_REQUIRED) {
                return Response.error(ErrorCode.ADDITIONAL_INFO_REQUIRED);
            }
            throw e; //다른 에러는 그대로 전파
        }
    }

    @PostMapping("/signup")
    public Response<CustomerDetailsResponse> oauthSignup(@RequestBody OauthSignupRequest request) {
        CustomerDetailsResponse response = oAuthService.oauthSignup(request);
        return Response.success(response);
    }
*/
}
