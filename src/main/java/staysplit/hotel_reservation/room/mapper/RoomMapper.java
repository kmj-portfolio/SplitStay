package staysplit.hotel_reservation.room.mapper;

import org.springframework.stereotype.Component;
import staysplit.hotel_reservation.photo.domain.PhotoEntity;
import staysplit.hotel_reservation.photo.service.S3Service;
import staysplit.hotel_reservation.room.domain.RoomEntity;
import staysplit.hotel_reservation.room.dto.response.RoomInfoResponse;

import java.util.List;
import java.util.Optional;

@Component
public class RoomMapper {

    private S3Service s3Service;

    public RoomInfoResponse toRoomInfoResponse(RoomEntity room) {

        Optional<PhotoEntity> mainPhoto = room.getMainPhoto();

        String mainUrl = mainPhoto.isPresent() ? s3Service.getS3Url(mainPhoto.get().getStoredFileName()) : null;

        List < String > additionalUrls = room.getPhotos()
        .stream()
        .filter(photo -> !photo.isMainPhoto())
        .map(photo -> s3Service.getS3Url(photo.getStoredFileName()))
        .toList();

        return new RoomInfoResponse(
                room.getHotel().getId(),
                room.getHotel().getName(),
                room.getId(),
                room.getRoomType(),
                room.getDescription(),
                room.getMaxOccupancy(),
                room.getPrice(),
                room.getTotalQuantity(),
                mainUrl,
                additionalUrls);
    }
}
