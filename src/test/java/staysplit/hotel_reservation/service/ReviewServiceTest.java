package staysplit.hotel_reservation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import staysplit.hotel_reservation.common.exception.AppException;
import staysplit.hotel_reservation.common.exception.ErrorCode;
import staysplit.hotel_reservation.customer.domain.entity.CustomerEntity;
import staysplit.hotel_reservation.customer.repository.CustomerRepository;
import staysplit.hotel_reservation.hotel.entity.HotelEntity;
import staysplit.hotel_reservation.hotel.repository.HotelRepository;
import staysplit.hotel_reservation.review.domain.dto.request.CreateReviewRequest;
import staysplit.hotel_reservation.review.domain.dto.request.ModifyReviewRequest;
import staysplit.hotel_reservation.review.domain.entity.ReviewEntity;
import staysplit.hotel_reservation.review.repository.ReviewRepository;
import staysplit.hotel_reservation.review.service.ReviewService;

import java.util.Optional;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private HotelRepository hotelRepository;
    @Mock private CustomerRepository customerRepository;

    @InjectMocks private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("리뷰 생성 성공")
    void createReview_success() {
        CreateReviewRequest request = new CreateReviewRequest(1, 1, "홍길동", 10, "좋은 숙소입니다", 5);
        CustomerEntity customer = CustomerEntity.builder().id(1).nickname("홍길동").build();
        HotelEntity hotel = HotelEntity.builder().id(10).build();
        ReviewEntity review = ReviewEntity.builder().customer(customer).hotel(hotel).content("좋은 숙소입니다").rating(5).build();

        when(reviewRepository.existsByUserIdAndHotelId(1, 10)).thenReturn(false);
        when(hotelRepository.findById(10)).thenReturn(Optional.of(hotel));
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(reviewRepository.save(any())).thenReturn(review);

        var response = reviewService.createReview(request);
        assertEquals("좋은 숙소입니다", response.content());
        assertEquals(5, response.rating());
    }

    @Test
    @DisplayName("리뷰 수정 성공")
    void modifyReview_success() {
        CustomerEntity customer = CustomerEntity.builder().id(1).nickname("홍길동").build();
        ReviewEntity review = ReviewEntity.builder().id(10).customer(customer).content("이전 내용").rating(3).build();

        ModifyReviewRequest request = new ModifyReviewRequest(1,"수정된 내용", 4);

        when(reviewRepository.findById(10)).thenReturn(Optional.of(review));

        var result = reviewService.modifyReview(10, request);

        assertEquals("수정된 내용", result.content());
        assertEquals(4, result.rating());
    }

    @Test
    @DisplayName("리뷰 삭제 성공")
    void deleteReview_success() {
        CustomerEntity customer = CustomerEntity.builder().id(1).nickname("홍길동").build();
        ReviewEntity review = ReviewEntity.builder().id(10).customer(customer).content("삭제 테스트").rating(5).build();

        when(reviewRepository.findById(10)).thenReturn(Optional.of(review));

        reviewService.deleteReview(1, 10);
        assertNotNull(review.getDeletedAt());
    }

    @Test
    @DisplayName("리뷰 수정 권한 없음")
    void modifyReview_unauthorized() {
        CustomerEntity customer = CustomerEntity.builder().id(2).build(); // 다른 사용자
        ReviewEntity review = ReviewEntity.builder().id(10).customer(customer).build();
        ModifyReviewRequest request = new ModifyReviewRequest(1, "수정", 5);

        when(reviewRepository.findById(10)).thenReturn(Optional.of(review));

        AppException exception = assertThrows(AppException.class,
                () -> reviewService.modifyReview(10, request));

        assertEquals(ErrorCode.UNAUTHORIZED_REVIEWER, exception.getErrorCode());
    }

    @Test
    @DisplayName("리뷰 조회 by 유저 성공")
    void getReviewByUserId_success() {
        ReviewEntity review = ReviewEntity.builder().content("좋아요").rating(5).build();
        Page<ReviewEntity> page = new PageImpl<>(Collections.singletonList(review));

        when(reviewRepository.getReviewByCustomerId(eq(1), any())).thenReturn(page);

        var result = reviewService.getReviewByCustomerId(1, PageRequest.of(0, 10));
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("리뷰 조회 by 호텔 성공")
    void getReviewByHotelId_success() {
        ReviewEntity review = ReviewEntity.builder().content("최고에요").rating(4).build();
        Page<ReviewEntity> page = new PageImpl<>(Collections.singletonList(review));

        when(reviewRepository.getReviewByHotelId(eq(1), any())).thenReturn(page);

        var result = reviewService.getReviewByHotelId(1, PageRequest.of(0, 10));
        assertEquals(1, result.getTotalElements());
    }
}
