package staysplit.hotel_reservation.hotelSearch.dto.request;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record HotelSearchCondition(
        LocalDate checkIn,
        LocalDate checkOut,
        Double longitude,
        Double latitude,
        Integer numGuest,
        Integer minPrice,
        Integer maxPrice,
        List<Integer> numStar
) {
}
