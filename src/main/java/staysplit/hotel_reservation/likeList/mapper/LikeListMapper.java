package staysplit.hotel_reservation.likeList.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;
import staysplit.hotel_reservation.hotel.dto.response.GetHotelListResponse;
import staysplit.hotel_reservation.hotel.mapper.HotelMapper;
import staysplit.hotel_reservation.likeList.dto.response.LikeListCollectionResponse;
import staysplit.hotel_reservation.likeList.dto.response.LikeListDetailResponse;
import staysplit.hotel_reservation.likeList.domain.entity.LikeListEntity;
import staysplit.hotel_reservation.photo.service.PhotoUrlBuilder;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LikeListMapper {

    private final HotelMapper hotelMapper;
    private final PhotoUrlBuilder urlBuilder;

    // N + 1 (get Customer entity)
    public LikeListDetailResponse toDetailResponse(LikeListEntity likeList) {

        List<GetHotelListResponse> hotelList = likeList.getHotels().stream()
                .map(h -> hotelMapper.toListResponse(h.getHotel(), urlBuilder))
                .toList();

        Page<GetHotelListResponse> page = new PageImpl<>(hotelList);

        return LikeListDetailResponse.builder()
                .listName(likeList.getListName())
                .ownerNickname(likeList.getOwner().getNickname())
                .participantUsernames(likeList.getParticipants().stream().map(p -> p.getCustomer().getNickname()).toList())
                .hotels(page)
                .build();
    }

    public List<LikeListCollectionResponse> toCollectionResponse(List<LikeListEntity> list) {
        return list.stream()
                .map(ll -> LikeListCollectionResponse.builder()
                        .likeListId(ll.getId())
                        .listName(ll.getListName())
                        .ownerNickname(ll.getOwner().getNickname())
                        .numberOfParticipants(ll.getParticipants().size())
                        .build())
                .toList();
    }


}
