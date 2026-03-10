package staysplit.hotel_reservation.photo.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import staysplit.hotel_reservation.common.exception.AppException;
import staysplit.hotel_reservation.common.exception.ErrorCode;
import staysplit.hotel_reservation.hotel.entity.HotelEntity;
import staysplit.hotel_reservation.hotel.service.HotelValidator;
import staysplit.hotel_reservation.photo.domain.PhotoEntity;
import staysplit.hotel_reservation.photo.domain.enums.DisplayType;
import staysplit.hotel_reservation.photo.dto.response.PhotoDetailResponse;
import staysplit.hotel_reservation.photo.repository.PhotoRepository;
import staysplit.hotel_reservation.provider.domain.entity.ProviderEntity;
import staysplit.hotel_reservation.provider.service.ProviderValidator;
import staysplit.hotel_reservation.room.domain.RoomEntity;
import staysplit.hotel_reservation.room.service.RoomValidator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class PhotoUploadService {

    private final PhotoRepository photoRepository;
    private final ProviderValidator providerValidator;
    private final HotelValidator hotelValidator;
    private final RoomValidator roomValidator;
    private final S3Service s3Service;

    public List<PhotoDetailResponse> uploadPhotos(String email, String entityType, Integer entityId, List<String> displayType,
                                                  List<MultipartFile> multipartFiles) throws IOException {
        List<PhotoDetailResponse> storedFileList = new ArrayList<>();
        for (int i = 0; i < displayType.size(); i++) {
            PhotoDetailResponse photoDetailResponse = uploadPhoto(email, entityType, entityId, displayType.get(i), multipartFiles.get(i));
            storedFileList.add(photoDetailResponse);
        }
        return storedFileList;
    }

    public PhotoDetailResponse uploadPhoto(String email, String entityType, Integer entityId, String displayTypeValue,
                                           MultipartFile multipartFile) throws IOException {

        ProviderEntity provider = providerValidator.validateProvider(email);

        String originalFileName = multipartFile.getOriginalFilename();
        String storedFileName = createStoredFileName(originalFileName);

        // multipartFile.transferTo(new File(getFullPath(storedFileName)));
        s3Service.upload(storedFileName, multipartFile);

        DisplayType displayType = DisplayType.from(displayTypeValue);

        PhotoEntity photo = PhotoEntity.builder()
                .displayType(displayType)
                .uploadFileName(originalFileName)
                .storedFileName(storedFileName)
                .build();

        photoRepository.save(photo);

        HotelEntity hotel;
        RoomEntity room;

        if (entityType.equals("HOTEL")) {
            hotel = hotelValidator.validateHotel(entityId);
            associatePhotoWithHotel(hotel, photo);

        } else if (entityType.equals("ROOM")) {
            room = roomValidator.validateRoom(entityId);
            associatePhotoWithRoom(room, photo);

        } else {
            throw new AppException(ErrorCode.INVALID_ENTITY_TYPE, ErrorCode.INVALID_ENTITY_TYPE.getMessage());
        }

        return PhotoDetailResponse.from(photo, s3Service.getS3Url(photo.getStoredFileName()));
    }

    // 사진 삭제
    public void deletePhoto(String filename) {
        PhotoEntity photo = photoRepository.findByStoredFileName(filename)
                .orElseThrow(() -> new AppException(ErrorCode.PHOTO_NOT_FOUND, ErrorCode.PHOTO_NOT_FOUND.getMessage()));

        s3Service.delete(filename);

        if (photo.isHotelPhoto()) {
            photo.getHotel().getPhotos().remove(photo);
        } else {
            photo.getRoom().getPhotos().remove(photo);
        }
    }

    private String createStoredFileName(String originalFileName) {
        String extension = extractExtension(originalFileName);
        String uuid = UUID.randomUUID().toString();
        return uuid + "." + extension;
    }

    private String extractExtension(String originalFileName) {
        int index = originalFileName.lastIndexOf(".");
        return originalFileName.substring(index + 1);
    }

    private void associatePhotoWithRoom(RoomEntity room, PhotoEntity photo) {
        if (photo.isMainPhoto()) {
            setMainPhotoForRoom(room, photo);
        } else {
            if (room.getPhotos().size() > 5) {
                throw new AppException(ErrorCode.EXCEEDED_PHOTO_LIMIT, ErrorCode.EXCEEDED_PHOTO_LIMIT.getMessage());
            }
            room.addPhoto(photo);
        }
    }

    private void associatePhotoWithHotel(HotelEntity hotel, PhotoEntity photo) {
        if (photo.isMainPhoto()) {
            setMainPhotoForHotel(hotel, photo);
        } else {
            if (hotel.getPhotos().size() > 5) {
                throw new AppException(ErrorCode.EXCEEDED_PHOTO_LIMIT, ErrorCode.EXCEEDED_PHOTO_LIMIT.getMessage());
            }
            hotel.addPhoto(photo);
        }
    }

    private void setMainPhotoForRoom(RoomEntity room, PhotoEntity photo) {
        // find current main photo
        Optional<PhotoEntity> existingMain = room.getPhotos().stream()
                .filter(PhotoEntity::isMainPhoto)
                .findFirst();

        // if there is an existing main photo, delete
        existingMain.ifPresent(photoEntity -> room.getPhotos().remove(photoEntity));

        room.addPhoto(photo);
    }

    private void setMainPhotoForHotel(HotelEntity hotel, PhotoEntity photo) {
        // find current main photo
        Optional<PhotoEntity> existingMain = hotel.getPhotos().stream()
                .filter(PhotoEntity::isMainPhoto)
                .findFirst();

        // if there is an existing main photo, delete
        existingMain.ifPresent(photoEntity -> hotel.getPhotos().remove(photoEntity));

        hotel.addPhoto(photo);
    }

}