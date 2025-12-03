package staysplit.hotel_reservation.hotelSearch.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HotelSearchCondition {
    private LocalDate checkIn;
    private LocalDate checkOut;
    private Double longitude;
    private Double latitude;
    private Integer numGuest;
    private Integer minPrice;
    private Integer maxPrice;
    private Integer numStar;
}
