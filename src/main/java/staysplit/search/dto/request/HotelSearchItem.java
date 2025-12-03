package staysplit.search.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HotelSearchItem {
    private Integer hotelId;
    private String hotelName;
    private String mainPhotoStoredFileName;
    private Integer minPrice;
    private Integer starLevel;
    private Double rating;
    private Integer reviewCount;
    private Double distanceMeters;
}
