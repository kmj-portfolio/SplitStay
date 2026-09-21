package staysplit.hotel_reservation.hotelSearch.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import staysplit.hotel_reservation.hotel.dto.response.GetHotelListResponse;
import staysplit.hotel_reservation.hotel.entity.HotelEntity;
import staysplit.hotel_reservation.hotel.mapper.HotelMapper;
import staysplit.hotel_reservation.hotelSearch.dto.request.HotelSearchCondition;
import staysplit.hotel_reservation.hotelSearch.repository.HotelSearchRepository;
import staysplit.hotel_reservation.room.repository.RoomRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotelSearchService {
    private final HotelSearchRepository hotelSearchRepository;
    private final HotelMapper hotelMapper;
    private final RoomRepository roomRepository;

    /*
    public Slice<GetHotelListResponse> searchHotels(HotelSearchCondition condition, Pageable pageable) {
        long serviceStart = System.nanoTime();
        long repositoryStart = System.nanoTime();
        Slice<HotelEntity> hotelEntities = hotelSearchRepository.searchNearbyHotels(condition, pageable);
        long repositoryElapsedMs = (System.nanoTime() - repositoryStart) / 1_000_000;
        log.info("[HotelSearch] repository.searchNearbyHotels = {} ms", repositoryElapsedMs);

        List<Integer> hotelIds = hotelEntities.getContent().stream()
                .map(HotelEntity::getId)
                .toList();

        long batchMinPriceStart = System.nanoTime();
        Map<Integer, Integer> minPriceByHotelId = roomRepository.findMinPriceByHotelIds(hotelIds)
                .stream()
                .collect(Collectors.toMap(
                        RoomRepository.HotelMinPrice::getHotelId,
                        RoomRepository.HotelMinPrice::getMinPrice
                ));
        long batchMinPriceElapsedMs = (System.nanoTime() - batchMinPriceStart) / 1_000_000;
        log.info("[HotelSearch] batchMinPrice = {} ms", batchMinPriceElapsedMs);

        // DTO 매핑 시간
        long mappingStart = System.nanoTime();
        Slice<GetHotelListResponse> responses = hotelEntities.map(hotel -> hotelMapper.toListResponse(hotel, minPriceByHotelId));
        long mappingElapsedMs = (System.nanoTime() - mappingStart) / 1_000_000;
        log.info("[HotelSearch] mapping = {} ms", mappingElapsedMs);

        // Service 전체 시간
        long serviceElapsedMs = (System.nanoTime() - serviceStart) / 1_000_000;
        log.info(
                "[HotelSearch] total service = {} ms " +
                        "(repository={} ms, batchMinPrice={} ms, mapping={} ms)",
                serviceElapsedMs,
                repositoryElapsedMs,
                batchMinPriceElapsedMs,
                mappingElapsedMs
        );

        return responses;
    }*/

    public Slice<GetHotelListResponse> searchHotels(HotelSearchCondition condition, Pageable pageable) {
        Slice<HotelEntity> hotelEntities = hotelSearchRepository.searchNearbyHotels(condition, pageable);

        List<Integer> hotelIds = hotelEntities.getContent().stream()
                .map(HotelEntity::getId)
                .toList();

        Map<Integer, Integer> minPriceByHotelId = roomRepository.findMinPriceByHotelIds(hotelIds)
                .stream()
                .collect(Collectors.toMap(
                        RoomRepository.HotelMinPrice::getHotelId,
                        RoomRepository.HotelMinPrice::getMinPrice
                ));

        Slice<GetHotelListResponse> responses = hotelEntities.map(hotel -> hotelMapper.toListResponse(hotel, minPriceByHotelId));

        return responses;
    }
}
