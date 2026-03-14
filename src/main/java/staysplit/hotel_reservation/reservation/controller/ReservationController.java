package staysplit.hotel_reservation.reservation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import staysplit.hotel_reservation.common.entity.Response;
import staysplit.hotel_reservation.reservation.dto.request.CreateReservationRequest;
import staysplit.hotel_reservation.reservation.dto.response.ReservationDetailResponse;
import staysplit.hotel_reservation.reservation.dto.response.ReservationListResponse;
import staysplit.hotel_reservation.reservation.domain.enums.ReservationStatus;
import staysplit.hotel_reservation.reservation.service.ReservationQueryService;
import staysplit.hotel_reservation.reservation.service.ReservationService;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;
    private final ReservationQueryService reservationQueryService;

    @GetMapping
    public Response<Page<ReservationListResponse>> findAllReservationsByCustomer(Authentication authentication,
                                                                                 @Param(value = "status") ReservationStatus status,
                                                                                 @Param(value = "afterDate") LocalDate afterDate,
                                                                                 Pageable pageable) {
        Page<ReservationListResponse> responseList = reservationQueryService.findAllReservationsByCustomer(authentication.getName(), status, afterDate, pageable);
        return Response.success(responseList);
    }

    @GetMapping("/{reservationId}")
    public Response<ReservationDetailResponse> findReservationById(Authentication authentication,
                                                                    @PathVariable Integer reservationId) {
        ReservationDetailResponse response = reservationQueryService.findReservationByReservationId(authentication.getName(), reservationId);
        return Response.success(response);
    }

    @PostMapping
    public Response<ReservationDetailResponse> makeTempReservation(Authentication authentication, @RequestBody CreateReservationRequest request) {
        ReservationDetailResponse response = reservationService.makeTempReservation(authentication.getName(), request);
        return Response.success(response);
    }

    @PostMapping("/confirmation/{reservationId}")
    public Response<ReservationDetailResponse> confirmReservationAfterPayment(@PathVariable Integer reservationId) {
        ReservationDetailResponse response = reservationService.confirmReservationAfterPayment(reservationId);
        return Response.success(response);
    }

    @PutMapping("/{reservationId}")
    public Response<String> cancelReservation(@PathVariable Integer reservationId, Authentication authentication) {
        reservationService.cancelReservation(authentication.getName(), reservationId);
        return Response.success("예약이 취소되었습니다.");
    }
}
