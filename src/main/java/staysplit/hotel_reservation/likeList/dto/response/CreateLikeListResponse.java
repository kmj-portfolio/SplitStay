package staysplit.hotel_reservation.likeList.dto.response;

public record CreateLikeListResponse(
        Integer listId,
        Integer customerId, // 목록 주인
        String listName
) {
}
