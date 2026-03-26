package staysplit.hotel_reservation.dataInitializer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staysplit.hotel_reservation.common.exception.AppException;
import staysplit.hotel_reservation.common.exception.ErrorCode;
import staysplit.hotel_reservation.hotel.entity.HotelEntity;
import staysplit.hotel_reservation.hotel.repository.HotelRepository;
import staysplit.hotel_reservation.provider.domain.entity.ProviderEntity;
import staysplit.hotel_reservation.provider.repository.ProviderRepository;
import staysplit.hotel_reservation.room.domain.RoomEntity;
import staysplit.hotel_reservation.room.repository.RoomRepository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotelAndRoomCsvImportService {

    private final HotelRepository hotelRepository;
    private final ProviderRepository providerRepository;
    private final RoomRepository roomRepository;

    @Transactional
    public void importHotelCsv() {
        try {
            ClassPathResource resource = new ClassPathResource("data/hotels.csv");

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
                    String email = record.get("email");
                    String hotelName = record.get("name");
                    int starLevel = Integer.parseInt(record.get("star"));
                    String address = record.get("address");
                    double longitude = Double.parseDouble(record.get("longitude"));
                    double latitude = Double.parseDouble(record.get("latitude"));
                    String hotelDescription = record.get("hotel_description");

                    String room1Name = record.get("room1_name");
                    int room1Price = Integer.parseInt(record.get("room1_price"));
                    int room1Occupancy = Integer.parseInt(record.get("room1_maxOccupancy"));
                    String room1Description = record.get("room1_description");
                    int room1Quantity = Integer.parseInt(record.get("room1_totalQuantity"));

                    ProviderEntity providerEntity = providerRepository.findByEmail(email)
                            .orElseThrow(() -> {
                                log.warn("[해당 이메일의 Provider를 찾을 수 없습니다] email={}", email);
                                return new AppException(ErrorCode.USER_NOT_FOUND);
                            });

                    HotelEntity hotelEntity = HotelEntity.builder()
                            .name(hotelName)
                            .starLevel(starLevel)
                            .address(address)
                            .longitude(longitude)
                            .latitude(latitude)
                            .description(hotelDescription)
                            .provider(providerEntity)
                            .build();

                    hotelRepository.save(hotelEntity);

                    providerEntity.addHotel(hotelEntity);

                    RoomEntity roomEntity1 = RoomEntity.builder()
                            .roomType(room1Name)
                            .price(room1Price)
                            .hotel(hotelEntity)
                            .description(room1Description)
                            .totalQuantity(room1Quantity)
                            .maxOccupancy(room1Occupancy)
                            .build();

                    roomRepository.save(roomEntity1);

                    String room2Name = record.get("room2_name");
                    if (room2Name != null && !room2Name.isBlank()) {
                        int room2Price = Integer.parseInt(record.get("room2_price"));
                        int room2occupancy = Integer.parseInt(record.get("room2_maxOccupancy"));
                        String room2Description = record.get("room2_description");
                        int room2Quantity = Integer.parseInt(record.get("room2_totalQuantity"));

                        RoomEntity roomEntity2 = RoomEntity.builder()
                                .roomType(room2Name)
                                .price(room2Price)
                                .hotel(hotelEntity)
                                .description(room2Description)
                                .totalQuantity(room2Quantity)
                                .maxOccupancy(room2occupancy)
                                .build();

                        roomRepository.save(roomEntity2);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Hotel CSV import 실패", e);
        }
    }
}
