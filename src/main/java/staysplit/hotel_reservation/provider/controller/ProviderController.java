package staysplit.hotel_reservation.provider.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import staysplit.hotel_reservation.common.entity.Response;
import staysplit.hotel_reservation.provider.domain.dto.reqeust.ProviderSignupRequest;
import staysplit.hotel_reservation.provider.domain.dto.response.ProviderDetailResponse;
import staysplit.hotel_reservation.provider.domain.dto.response.ProviderSignupResponse;
import staysplit.hotel_reservation.provider.service.ProviderService;
import staysplit.hotel_reservation.reservation.dto.response.ReservationListResponseForProvider;
import staysplit.hotel_reservation.reservation.reposiotry.search.ReservationSearchConditionForProviders;
import staysplit.hotel_reservation.reservation.service.ReservationQueryService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/providers")
public class ProviderController {

    private final ProviderService providerService;
    private final ReservationQueryService reservationQueryService;

    // 회원가입
    @PostMapping("/sign-up")
    public Response<ProviderSignupResponse> signupAsProvider(@RequestBody ProviderSignupRequest request) {
        ProviderSignupResponse response = providerService.signup(request);
        return Response.success(response);
    }

    // 마이페이지
    @GetMapping("/profile")
    public Response<ProviderDetailResponse> getProviderInfo(Authentication authentication) {
        ProviderDetailResponse myPage = providerService.getMyPage(authentication.getName());
        return Response.success(myPage);
    }

    // 호텔의 모든 예약 조회
    @PostMapping("/reservations")
    public Response<Page<ReservationListResponseForProvider>> findAllReservations(@RequestBody ReservationSearchConditionForProviders conditions,
                                                                                  @PageableDefault(size = 10, sort = "checkInStart", direction = Sort.Direction.DESC) Pageable pageable,
                                                                                  Authentication authentication) {
        Page<ReservationListResponseForProvider> response = reservationQueryService.findAllReservationsToHotel(authentication.getName(), pageable, conditions);
        return Response.success(response);
    }


}
