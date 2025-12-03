package staysplit.hotel_reservation.hotelSearch.repository;

import org.springframework.data.domain.*;
import staysplit.hotel_reservation.hotel.entity.HotelEntity;
import staysplit.hotel_reservation.hotelSearch.dto.request.HotelSearchCondition;

public interface HotelSearchRepository {
    Page<HotelEntity> searchNearbyHotels(HotelSearchCondition condition, Pageable pageable);
}
