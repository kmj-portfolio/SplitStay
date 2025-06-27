package staysplit.hotel_reservation.likes.domain.dto.response;

public record CreateLikeListResponse(
        Integer listId,
        Integer customerId,
        String listName
) {
}
