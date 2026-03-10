package staysplit.hotel_reservation.photo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class PhotoUploadServiceTest {

    @Mock private PhotoRepository photoRepository;
    @Mock private ProviderValidator providerValidator;
    @Mock private HotelValidator hotelValidator;
    @Mock private RoomValidator roomValidator;
    @Mock private S3Service s3Service;

    @InjectMocks private PhotoUploadService photoUploadService;

    private final String PROVIDER_EMAIL = "provider@example.com";
    private final String ORIGINAL_FILE_NAME = "original.png";
    private final String STORED_FILE_NAME = "123456789.png";
    private final String S3_URL = "https://bucket.s3.ap-northeast-2.amazonaws.com/uuid.png";

    private ProviderEntity provider;
    private HotelEntity hotel;
    private RoomEntity room;
    private MultipartFile mockFile;

    @BeforeEach
    void setUp() {
        provider = mock(ProviderEntity.class);
        hotel = HotelEntity.builder()
                .id(1)
                .name("Test Hotel")
                .address("Test Address")
                .longitude(127.0)
                .latitude(37.0)
                .build();

        room = RoomEntity.builder()
                .roomType("STANDARD")
                .maxOccupancy(2)
                .price(100000)
                .totalQuantity(10)
                .build();

        mockFile = mock(MultipartFile.class);
    }

    @Nested
    @DisplayName("단일 사진 업로드")
    class UploadPhoto {

        @Test
        @DisplayName("성공 - 호텔 대표 사진 업로드")
        void uploadHotelMainPhoto_success() throws IOException {
            given(providerValidator.validateProvider(PROVIDER_EMAIL)).willReturn(provider);
            given(mockFile.getOriginalFilename()).willReturn(ORIGINAL_FILE_NAME);
            given(hotelValidator.validateHotel(1)).willReturn(hotel);
            given(s3Service.getS3Url(anyString())).willReturn(S3_URL);

            PhotoDetailResponse response = photoUploadService.uploadPhoto(
                    PROVIDER_EMAIL, "HOTEL", 1, "MAIN", mockFile);

            assertThat(response.photoUrl()).isEqualTo(S3_URL);
            assertThat(response.displayType()).isEqualTo(DisplayType.MAIN.toString());

            then(photoRepository).should().save(any(PhotoEntity.class));
            then(s3Service).should().upload(anyString(), any(MultipartFile.class));
        }

        @Test
        @DisplayName("성공 - 호텔 추가 사진 업로드")
        void uploadAdditionalHotelPhotos_success() throws IOException {
            given(providerValidator.validateProvider(PROVIDER_EMAIL)).willReturn(provider);
            given(mockFile.getOriginalFilename()).willReturn(ORIGINAL_FILE_NAME);
            given(hotelValidator.validateHotel(1)).willReturn(hotel);
            given(s3Service.getS3Url(anyString())).willReturn(S3_URL);

            PhotoDetailResponse response = photoUploadService.uploadPhoto(
                    PROVIDER_EMAIL, "HOTEL", 1, "ADDITIONAL", mockFile);

            assertThat(response.photoUrl()).isEqualTo(S3_URL);
            assertThat(response.displayType()).isEqualTo(DisplayType.ADDITIONAL.toString());

            then(photoRepository).should().save(any(PhotoEntity.class));
            then(s3Service).should().upload(anyString(), any(MultipartFile.class));
        }

        @Test
        @DisplayName("성공 - 방 대표 사진 업로드")
        void uploadRoomMainPhoto_success() throws IOException {
            given(providerValidator.validateProvider(PROVIDER_EMAIL)).willReturn(provider);
            given(mockFile.getOriginalFilename()).willReturn(ORIGINAL_FILE_NAME);
            given(roomValidator.validateRoom(1)).willReturn(room);

            PhotoDetailResponse response = photoUploadService.uploadPhoto(
                    PROVIDER_EMAIL, "ROOM",  1, "MAIN", mockFile);

            assertThat(response.photoUrl()).isEqualTo(S3_URL);
            assertThat(response.displayType()).isEqualTo(DisplayType.MAIN.toString());
            assertThat(response.uploadedFileName()).isEqualTo(ORIGINAL_FILE_NAME);

            then(photoRepository).should().save(any(PhotoEntity.class));
            then(s3Service).should().upload(anyString(), any(MultipartFile.class));
        }

        @Test
        @DisplayName("실패 - 잘못된 entityType 입력")
        void uploadPhoto_invalidEntityType_throwsException() throws IOException {
            given(providerValidator.validateProvider(PROVIDER_EMAIL)).willReturn(provider);
            given(mockFile.getOriginalFilename()).willReturn(ORIGINAL_FILE_NAME);

            assertThatThrownBy(() -> photoUploadService.uploadPhoto(
                    PROVIDER_EMAIL, "INVALID_ENTITY_TYPE",  1, "MAIN", mockFile))
                    .isInstanceOf(AppException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_ENTITY_TYPE);
        }

        @Test
        @DisplayName("성공 - 이미 존재하는 호텔 MAIN 파일 교체")
        void uploadHotelMainPhoto_replacesExistingMain() throws IOException {
            PhotoEntity existingHotelMainPhoto = PhotoEntity.builder()
                    .uploadFileName("old.png")
                    .storedFileName("old-uuid.png")
                    .displayType(DisplayType.MAIN)
                    .build();

            hotel.addPhoto(existingHotelMainPhoto);

            given(providerValidator.validateProvider(PROVIDER_EMAIL)).willReturn(provider);
            given(mockFile.getOriginalFilename()).willReturn("new.png");
            given(hotelValidator.validateHotel(1)).willReturn(hotel);

            PhotoDetailResponse response = photoUploadService.uploadPhoto(
                    PROVIDER_EMAIL, "HOTEL", 1, "MAIN", mockFile);

            List<PhotoEntity> photos = hotel.getPhotos();
            long mainPhotoCount = photos.stream()
                            .filter(PhotoEntity::isMainPhoto)
                            .count();

            assertThat(mainPhotoCount).isEqualTo(1);
            assertThat(photos).doesNotContain(existingHotelMainPhoto);
            assertThat(photos.get(0).getUploadFileName()).isEqualTo("new.png");
        }
    }

}