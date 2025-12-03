package staysplit.hotel_reservation.hotelSearch.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import staysplit.hotel_reservation.hotel.dto.response.GetHotelListResponse;
import staysplit.hotel_reservation.hotel.entity.HotelEntity;
import staysplit.hotel_reservation.hotel.mapper.HotelMapper;
import staysplit.hotel_reservation.hotelSearch.dto.request.HotelSearchCondition;
import staysplit.hotel_reservation.hotelSearch.repository.HotelSearchRepository;
import staysplit.hotel_reservation.photo.service.PhotoUrlBuilder;


@Service
@RequiredArgsConstructor
public class HotelSearchService {
    private final HotelSearchRepository hotelSearchRepository;
    private final HotelMapper hotelMapper;
    private final PhotoUrlBuilder photoUrlBuilder;

    public Page<GetHotelListResponse> searchHotels(HotelSearchCondition condition, Pageable pageable) {
        Page<HotelEntity> hotelEntities = hotelSearchRepository.searchNearbyHotels(condition, pageable);
        Page<GetHotelListResponse> responses = hotelEntities.map(hotel -> hotelMapper.toListResponse(hotel, photoUrlBuilder));
        return responses;
    }
}
