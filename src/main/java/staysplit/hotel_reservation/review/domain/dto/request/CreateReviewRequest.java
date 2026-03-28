package staysplit.hotel_reservation.review.domain.dto.request;

public record CreateReviewRequest(
   Integer customerId,
   Integer hotelId,
   String content,
   Integer rating
) {}
