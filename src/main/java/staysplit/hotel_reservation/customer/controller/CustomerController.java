package staysplit.hotel_reservation.customer.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import staysplit.hotel_reservation.common.entity.Response;
import staysplit.hotel_reservation.customer.domain.dto.request.NicknameChangeRequest;
import staysplit.hotel_reservation.customer.domain.dto.request.CustomerSignupRequest;
import staysplit.hotel_reservation.customer.domain.dto.response.CustomerDetailsResponse;
import staysplit.hotel_reservation.customer.service.CustomerService;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    // 일반 회원가입
    @PostMapping("/sign-up")
    public Response<CustomerDetailsResponse> signup(@Valid @RequestBody CustomerSignupRequest request) {
        CustomerDetailsResponse customerDetailsResponse = customerService.signup(request);
        return Response.success(customerDetailsResponse);
    }

    // 사용자 프로필 조회
    @GetMapping("/my")
    public Response<CustomerDetailsResponse> getMyInfo(Authentication authentication) {
        CustomerDetailsResponse customerDetailsResponse = customerService.getMyProfile(authentication.getName());
        return Response.success(customerDetailsResponse);
    }

    // 사용자 정보 조회 - Admin
    @GetMapping("/{id}")
    public Response<CustomerDetailsResponse> getUserDetails(@PathVariable Integer id) {
        CustomerDetailsResponse customerDetailsResponse = customerService.findCustomerById(id);
        return Response.success(customerDetailsResponse);
    }

    // 사용자 이름 수정
    @PutMapping("/nickname")
    public Response<CustomerDetailsResponse> changeNickname(@RequestBody NicknameChangeRequest request, Authentication authentication) {
        CustomerDetailsResponse customerDetailsResponse = customerService.changeNickname(request, authentication.getName());
        return Response.success(customerDetailsResponse);
    }

    // 내 계정 삭제
    @DeleteMapping("/my")
    public Response<String> delete(Authentication authentication) {
        customerService.delete(authentication.getName());
        return Response.success("계정이 삭제되었습니다.");
    }
}
