package staysplit.hotel_reservation.dataInitializer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import staysplit.hotel_reservation.common.exception.AppException;
import staysplit.hotel_reservation.common.exception.ErrorCode;
import staysplit.hotel_reservation.hotel.entity.HotelEntity;
import staysplit.hotel_reservation.hotel.repository.HotelRepository;
import staysplit.hotel_reservation.photo.domain.PhotoEntity;
import staysplit.hotel_reservation.photo.domain.enums.DisplayType;
import staysplit.hotel_reservation.photo.repository.PhotoRepository;
import staysplit.hotel_reservation.room.domain.RoomEntity;
import staysplit.hotel_reservation.room.repository.RoomRepository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PhotoCsvInitializer {

    private final PhotoRepository photoRepository;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;

    @Transactional
    public void importPhotoFromCsv() {
        try {
            ClassPathResource resource = new ClassPathResource("data/photos.csv");

            try (
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)
                    );
                    CSVParser csvParser = new CSVParser(
                            reader,
                            CSVFormat.DEFAULT.builder()
                                    .setHeader()
                                    .setSkipHeaderRecord(true)
                                    .build()
                    )
            ) {
                for (CSVRecord record : csvParser) {
                    String hotelName = record.get("hotel_name");
                    String entityType = record.get("entity_type");
                    String fileName = record.get("upload_file_name");

                    HotelEntity hotel = hotelRepository.findByName(hotelName)
                            .orElseThrow(() -> {
                                log.info("hotel not found with name {}", hotelName);
                                return new AppException(ErrorCode.HOTEL_NOT_FOUND);
                            });


                    if (entityType.equals("HOTEL")) {
                        PhotoEntity photo = PhotoEntity.builder()
                                .uploadFileName(fileName)
                                .storedFileName(fileName)
                                .displayType(DisplayType.MAIN)
                                .hotel(hotel)
                                .build();

                        photoRepository.save(photo);

                        hotel.addPhoto(photo);

                    } else {
                        List<RoomEntity> rooms = roomRepository.findByHotelId(hotel.getId());

                        for (RoomEntity room : rooms) {
                            if (room.getPhotos().isEmpty()) {
                                PhotoEntity photo = PhotoEntity.builder()
                                        .uploadFileName(fileName)
                                        .storedFileName(fileName)
                                        .displayType(DisplayType.MAIN)
                                        .room(room)
                                        .build();

                                photoRepository.save(photo);

                                room.addPhoto(photo);
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Photo CSV import 실패", e);
        }
    }
}

