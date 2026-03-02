package staysplit.hotel_reservation.likeList.dto.response;

import java.util.List;

public record LikeListParticipantsResponse(
        Integer likeListId,
        String ownerNickname,
        List<String> participants
) {
}
