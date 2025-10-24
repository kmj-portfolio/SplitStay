package staysplit.hotel_reservation.hotel.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import staysplit.hotel_reservation.common.entity.Response;
import staysplit.hotel_reservation.hotel.dto.request.*;
import staysplit.hotel_reservation.hotel.dto.response.*;
import staysplit.hotel_reservation.hotel.service.HotelService;
import staysplit.hotel_reservation.room.dto.response.RoomInfoResponse;
import staysplit.hotel_reservation.room.service.RoomService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hotels")
public class HotelController {

    private final HotelService hotelService;
    private final RoomService roomService;

    //호텔 생성
    @PostMapping("/")
    public Response<CreateHotelResponse> createHotel(@RequestBody CreateHotelRequest request, Authentication authentication){
        CreateHotelResponse createHotelResponse = hotelService.createHotel(request, authentication.getName());
        return Response.success(createHotelResponse);
    }

    //호텔 수정
    @PutMapping("/{hotelId}")
    public Response<GetHotelDetailResponse> updateHotel(
            @PathVariable Integer hotelId,
            @RequestBody UpdateHotelRequest request,
            Authentication authentication){

        GetHotelDetailResponse response = hotelService.updateHotel(hotelId, request, authentication.getName());
        return Response.success(response);
    }

    //호텔 상세 조회
    @GetMapping("/{hotelId}")
    public Response<GetHotelDetailResponse> getHotelDetail(@PathVariable Integer hotelId){
        GetHotelDetailResponse response = hotelService.getHotelDetails(hotelId);
        return Response.success(response);
    }

    //호텔 목록 조회
    @GetMapping("/list")
    public Response<Page<GetHotelListResponse>> getHotelList(@PageableDefault(size = 10) Pageable pageable) {
        Page<GetHotelListResponse> response = hotelService.getHotelList(pageable);
        return Response.success(response);
    }

    //호텔 삭제
    @DeleteMapping("/{hotelId}")
    public Response<DeleteHotelResponse> deleteHotel(@PathVariable Integer hotelId, Authentication authentication){

        DeleteHotelResponse response = hotelService.deleteHotel(hotelId, authentication.getName());
        return Response.success(response);
    }

    // 호텔의 모든 방 조회
    @GetMapping("/{hotelId}/rooms")
    public Response<Page<RoomInfoResponse>> findAllRoomsByHotelId(@PathVariable Integer hotelId,
                                                                  @PageableDefault(size = 10, sort = "price", direction = Sort.Direction.ASC)
                                                                  Pageable pageable) {
        Page<RoomInfoResponse> rooms = roomService.findAllRoomsByHotel(hotelId, pageable);
        return Response.success(rooms);
    }
}