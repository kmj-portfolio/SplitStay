package staysplit.hotel_reservation.likeList.dto.response;

public record AddHotelResponse(
        Integer likeListId,
        Integer hotelId,
        String message
) {
    public AddHotelResponse(Integer likeListId, Integer hotelId) {
           this(likeListId, hotelId, "호텔이 목록에 추가되었습니다.");
    }
}
