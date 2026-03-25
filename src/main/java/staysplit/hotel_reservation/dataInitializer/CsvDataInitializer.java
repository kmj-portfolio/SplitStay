package staysplit.hotel_reservation.dataInitializer;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CsvDataInitializer implements CommandLineRunner {

    @Value("${app.init.enabled}")
    private boolean enabled;

    private final HotelAndRoomCsvImportService hotelAndRoomCsvImportService;
    private final ProviderCsvImportService providerCsvImportService;
    private final PhotoCsvInitializer photoCsvInitializer;

    @Override
    public void run(String... args) throws Exception {
        if (!enabled) {
            return;
        }
        //providerCsvImportService.importProvidersFromCsv();
        //hotelAndRoomCsvImportService.importHotelCsv();
        photoCsvInitializer.importPhotoFromCsv();
    }
}
