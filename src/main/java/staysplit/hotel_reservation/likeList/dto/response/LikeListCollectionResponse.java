package staysplit.hotel_reservation.likeList.dto.response;

import lombok.Builder;

@Builder
public record LikeListCollectionResponse(
        Integer likeListId,
        String listName,
        String ownerNickname,
        Integer numberOfParticipants
) {
}
