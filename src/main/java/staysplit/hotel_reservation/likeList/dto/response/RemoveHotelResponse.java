package staysplit.hotel_reservation.likeList.dto.response;

public record RemoveHotelResponse(
        Integer likeListId,
        Integer hotelId,
        String message
){
    public RemoveHotelResponse(Integer likeListId, Integer hotelId) {
        this(likeListId, hotelId, "호텔이 목록에서 삭제되었습니다.");
    }
}