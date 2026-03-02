package staysplit.hotel_reservation.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import retrofit2.http.HTTP;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    OAUTH_PROVIDER_ERROR(HttpStatus.CONFLICT, "구글 토큰 에러"),
    // USER
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "중복되는 닉네임입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "사용자의 이메일이 중복됩니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),
    INVALID_PASSWORD(HttpStatus.CONFLICT, "비밀번호가 틀립니다."),
    ADDITIONAL_INFO_REQUIRED(HttpStatus.UNAUTHORIZED, "추가 정보 입력이 필요합니다."),
    DUPLICATE_SOCIAL_ID(HttpStatus.CONFLICT, "이미 가입된 계정입니다."),
    PROVIDER_ALREADY_REGISTERED(HttpStatus.CONFLICT, "이미 존재하는 공급자입니다"),
    USER_NOT_LOGGED_IN(HttpStatus.UNAUTHORIZED, "로그인 상태가 아닙니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "토큰이 유효하지 않습니다."),
    NO_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Refresh Token이 만료되었습니다. 다시 로그인해 주세요."),

    // Room
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 방입니다"),
    UNAUTHORIZED_PROVIDER(HttpStatus.UNAUTHORIZED, "이 방에 대한 권한이 없습니다"),

    // Hotel
    HOTEL_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 호텔입니다"),

    // Review
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 리뷰입니다."),
    UNAUTHORIZED_REVIEWER(HttpStatus.UNAUTHORIZED, "리뷰 작성자가 아닙니다."),

    // Payment
    DUPLICATE_PAYMENT(HttpStatus.CONFLICT, "이미 처리된 결제입니다."),
    INVALID_PAYMENT(HttpStatus.BAD_REQUEST, "결제 정보를 찾을 수 없습니다."),
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "결제 금액이 일치하지 않습니다."),
    INVALID_PAYMENT_STATUS(HttpStatus.BAD_REQUEST, "결제가 완료되지 않았습니다."),

    // Reservation
    INVALID_CHECKOUT_DATE(HttpStatus.CONFLICT, "체크아웃 날짜는 체크인 날짜보다 이후여야 합니다."),
    RESERVATION_KEY_NOT_FOUND(HttpStatus.NOT_FOUND, "유효하지 않은 결제 정보입니다."),
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND.NOT_FOUND, "존재하지 않는 예약입니다."),
    RESERVED_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "예약하지 않은 방입니다."),
    INSUFFICIENT_ROOM_STOCK(HttpStatus.CONFLICT, "방 재고가 부족합니다."),
    PAYMENT_INCOMPLETE_FOR_ALL_PARTICIPANTS(HttpStatus.UNAUTHORIZED, "모든 참여자들이 결제를 완료하지 않았습니다."),
    EXPIRED_RESERVATION(HttpStatus.CONFLICT, "이미 만료된 예약입니다."),

    // Photo
    INVALID_ENTITY_TYPE(HttpStatus.NOT_FOUND, "올바른 Entity Type을 입력해주세요 (HOTEL, ROOM)"),
    PHOTO_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사진입니다."),
    EXCEEDED_PHOTO_LIMIT(HttpStatus.CONFLICT, "사진은 5개까지만 등록할 수 있습니다."),

    // Cart
    ITEM_ALREADY_IN_CART(HttpStatus.CONFLICT, "이미 장바구니에 담긴 방입니다"),
    CART_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 장바구니 입니다"),
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "징바구니에 존재하지 않는 상품입니다"),
    UNAUTHORIZED_CUSTOMER(HttpStatus.UNAUTHORIZED, "사용자가 권한이 없습니다"),

    // LikeList
    LIKE_LIST_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 좋아요 목록입니다."),
    LIKE_LIST_PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND, "목록에 참여하고 있지 않은 사용자 입니다"),
    HOTEL_NOT_IN_LIKE_LIST(HttpStatus.NOT_FOUND, "좋아요에 존재하지 않는 호텔입니다"),
    HOTEL_ALREADY_IN_LIKE_LIST(HttpStatus.CONFLICT, "이미 좋아요한 호텔입니다."),
    SAME_EMAIL_AS_OWNER(HttpStatus.CONFLICT, "사용자가 자신의 이메일 주소를 입력했습니다.");

    private HttpStatus httpStatus;
    private String message;


}
