package staysplit.hotel_reservation.likeList.dto.response;

import lombok.Builder;
import org.springframework.data.domain.Page;
import staysplit.hotel_reservation.hotel.dto.response.GetHotelListResponse;

import java.util.List;

@Builder
public record LikeListDetailResponse(
        String listName,
        String ownerNickname,
        List<String> participantUsernames,
        Page<GetHotelListResponse> hotels
) {
}
