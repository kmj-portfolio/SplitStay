package staysplit.hotel_reservation.review.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import staysplit.hotel_reservation.common.entity.Response;
import staysplit.hotel_reservation.review.domain.dto.request.CreateReviewRequest;
import staysplit.hotel_reservation.review.domain.dto.request.ModifyReviewRequest;
import staysplit.hotel_reservation.review.domain.dto.response.CreateReviewResponse;
import staysplit.hotel_reservation.review.domain.dto.response.GetReviewResponse;
import staysplit.hotel_reservation.review.service.ReviewService;


@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping
    public Response<CreateReviewResponse> createReview(@RequestBody CreateReviewRequest request){
        CreateReviewResponse response = reviewService.createReview(request);
        return Response.success(response);
    }

    @GetMapping("/customers/{customerId}")
    public Response<Page<GetReviewResponse>> getReviewByCustomerId(
            @PathVariable Integer customerId,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<GetReviewResponse> review = reviewService.getReviewByCustomerId(customerId, pageable);
        return Response.success(review);
    }

    @GetMapping("/hotels/{hotelId}")
    public Response<Page<GetReviewResponse>> getReviewByHotelId(
            @PathVariable Integer hotelId,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<GetReviewResponse> review = reviewService.getReviewByHotelId(hotelId, pageable);
        return Response.success(review);
    }

    @DeleteMapping("/{reviewId}")
    public Response<String> deleteReview(@RequestParam Integer customerId, @PathVariable Integer reviewId) {
        reviewService.deleteReview(customerId, reviewId);
        return Response.success("리뷰가 삭제되었습니다.");
    }

    @PutMapping("/{reviewId}")
    public Response<String> modifyReview(@PathVariable Integer reviewId, @RequestBody ModifyReviewRequest request) {
        reviewService.modifyReview(reviewId, request);
        return Response.success("리뷰가 수정되었습니다.");
    }
}
