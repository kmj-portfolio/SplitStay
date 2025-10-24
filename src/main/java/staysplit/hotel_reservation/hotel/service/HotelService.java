package staysplit.hotel_reservation.hotel.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import staysplit.hotel_reservation.common.exception.AppException;
import staysplit.hotel_reservation.common.exception.ErrorCode;
import staysplit.hotel_reservation.hotel.dto.request.*;
import staysplit.hotel_reservation.hotel.dto.response.*;
import staysplit.hotel_reservation.hotel.entity.HotelEntity;
import staysplit.hotel_reservation.hotel.mapper.HotelMapper;
import staysplit.hotel_reservation.hotel.repository.HotelRepository;
import staysplit.hotel_reservation.photo.service.PhotoUrlBuilder;
import staysplit.hotel_reservation.provider.domain.entity.ProviderEntity;
import staysplit.hotel_reservation.provider.repository.ProviderRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import staysplit.hotel_reservation.room.repository.RoomRepository;

@Service
@Transactional
@RequiredArgsConstructor
public class HotelService {

    private final HotelRepository hotelRepository;
    private final ProviderRepository providerRepository;
    private final PhotoUrlBuilder photoUrlBuilder;
    private final HotelMapper mapper;
    private final RoomRepository roomRepository;

    public CreateHotelResponse createHotel(CreateHotelRequest request, String providerEmail) {
        ProviderEntity provider = validateProvider(providerEmail);

        HotelEntity hotel = HotelEntity.builder()
                .provider(provider)
                .name(request.name())
                .address(request.address())
                .longitude(request.longitude())
                .latitude(request.latitude())
                .description(request.description())
                .starLevel(request.starLevel())
                .build();

        provider.addHotel(hotel);
        hotelRepository.save(hotel);

        return mapper.toCreateHotelResponse(hotel);
    }

    @Transactional(readOnly = true)
    public GetHotelDetailResponse getHotelDetails(Integer hotelId) {
        HotelEntity hotel = getHotelOrThrow(hotelId);
        return mapper.toDetailResponse(hotel, photoUrlBuilder);
    }

    @Transactional(readOnly = true)
    public Page<GetHotelListResponse> getHotelList(Pageable pageable) {
        Page<HotelEntity> hotelPage = hotelRepository.findAll(pageable);
        return hotelPage.map(hotel -> mapper.toListResponse(hotel, photoUrlBuilder));
    }

    public GetHotelDetailResponse updateHotel(Integer hotelId, UpdateHotelRequest request, String providerEmail) {
        ProviderEntity provider = validateProvider(providerEmail);
        HotelEntity hotel = getHotelOrThrow(hotelId);

        verifyOwnership(hotel, provider);

        hotel.updateHotel(request);
        return mapper.toDetailResponse(hotel, photoUrlBuilder);
    }

    public DeleteHotelResponse deleteHotel(Integer hotelId, String email) {
        ProviderEntity provider = validateProvider(email);
        HotelEntity hotel = getHotelOrThrow(hotelId);

        verifyOwnership(hotel, provider);

        hotelRepository.delete(hotel);
        provider.addHotel(null);
        return new DeleteHotelResponse("호텔이 성공적으로 삭제되었습니다.", hotelId);
    }

    private ProviderEntity validateProvider(String email) {
        return providerRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, ErrorCode.USER_NOT_FOUND.getMessage()));
    }

    private HotelEntity getHotelOrThrow(Integer hotelId) {
        return hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "호텔을 찾을 수 없습니다."));
    }

    private void verifyOwnership(HotelEntity hotel, ProviderEntity provider) {
        if (!hotel.getProvider().getId().equals(provider.getId())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "이 호텔에 대한 권한이 없습니다.");
        }
    }
}