package staysplit.hotel_reservation.review.domain.dto.response;

import staysplit.hotel_reservation.review.domain.entity.ReviewEntity;

import java.time.LocalDateTime;

public record GetReviewResponse(
        Integer reviewId,
        Integer customerId,
        Integer hotelId,
        String hotelName,
        String nickname,
        String content,
        Integer rating,
        LocalDateTime createdAt
) {
    public static GetReviewResponse from(ReviewEntity review) {
        return new GetReviewResponse(
                review.getId(),
                review.geCustomerId(),
                review.getHotelId(),
                review.getHotel().getName(),
                review.getNickname(),
                review.getContent(),
                review.getRating(),
                review.getCreatedAt()
        );
    }
}
