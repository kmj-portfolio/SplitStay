package staysplit.hotel_reservation.hotel.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import staysplit.hotel_reservation.hotel.dto.response.CreateHotelResponse;
import staysplit.hotel_reservation.hotel.dto.response.GetHotelDetailResponse;
import staysplit.hotel_reservation.hotel.dto.response.GetHotelListResponse;
import staysplit.hotel_reservation.hotel.entity.HotelEntity;
import staysplit.hotel_reservation.photo.domain.PhotoEntity;
import staysplit.hotel_reservation.photo.service.S3Service;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class HotelMapper {

    private final S3Service s3Service;

    public GetHotelDetailResponse toDetailResponse(HotelEntity hotel) {

        Optional<PhotoEntity> mainPhoto = hotel.getMainPhoto();

        String mainUrl = mainPhoto.isPresent() ? s3Service.getS3Url(mainPhoto.get().getStoredFileName()) : null;
        List<String> additionalUrls = hotel.getPhotos()
                .stream()
                .filter(photo -> !photo.isMainPhoto())
                .map(photo -> s3Service.getS3Url(photo.getStoredFileName()))
                .toList();

        return new GetHotelDetailResponse(
                hotel.getId(),
                hotel.getName(),
                hotel.getAddress(),
                hotel.getLongitude(),
                hotel.getLatitude(),
                hotel.getDescription(),
                hotel.getStarLevel(),
                hotel.getRating(),
                hotel.getReviewCount(),
                mainUrl,
                additionalUrls
        );
    }

    public GetHotelListResponse toListResponse(HotelEntity hotel) {
        Optional<PhotoEntity> mainPhoto = hotel.getMainPhoto();

        String mainUrl = mainPhoto.isPresent() ? s3Service.getS3Url(mainPhoto.get().getStoredFileName()) : null;

        return new GetHotelListResponse(
                hotel.getId(),
                hotel.getName(),
                hotel.getAddress(),
                hotel.getStarLevel(),
                hotel.getRating(),
                hotel.getReviewCount(),
                mainUrl
        );
    }

    public CreateHotelResponse toCreateHotelResponse(HotelEntity hotel) {
        return new CreateHotelResponse(
                hotel.getId(),
                hotel.getName(),
                hotel.getAddress(),
                hotel.getLongitude(),
                hotel.getLatitude(),
                hotel.getDescription(),
                hotel.getStarLevel()
        );
    }
}