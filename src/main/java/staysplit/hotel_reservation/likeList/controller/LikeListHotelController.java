package staysplit.hotel_reservation.likeList.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import staysplit.hotel_reservation.common.entity.Response;
import staysplit.hotel_reservation.likeList.dto.response.AddHotelResponse;
import staysplit.hotel_reservation.likeList.dto.response.RemoveHotelResponse;
import staysplit.hotel_reservation.likeList.service.LikeListHotelService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/like-lists")
public class LikeListHotelController {

    private final LikeListHotelService likeListHotelService;

    @PostMapping("/{listId}/hotels/{hotelId}")
    public Response<AddHotelResponse> addHotel(Authentication authentication,
                                               @PathVariable Integer listId,
                                               @PathVariable Integer hotelId) {
        AddHotelResponse response = likeListHotelService.addHotelToLikeList(authentication.getName(), listId, hotelId);
        return Response.success(response);
    }

    @DeleteMapping("/{listId}/hotels/{hotelId}")
    public Response<RemoveHotelResponse> removeHotel(Authentication authentication,
                                                     @PathVariable Integer listId,
                                                     @PathVariable Integer hotelId) {
        RemoveHotelResponse response = likeListHotelService.removeHotelFromListList(authentication.getName(), listId, hotelId);
        return Response.success(response);
    }
}
