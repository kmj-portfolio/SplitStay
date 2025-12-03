package staysplit.search.repository;

import java.awt.print.Pageable;
import java.time.LocalDate;
import org.springframework.data.domain.*;
import staysplit.search.dto.request.HotelSearchItem;

public interface HotelSearchRepository {
    Page<HotelSearchItem> searchNearbyHotels(
            double userLat,
            double userLon,
            LocalDate checkIn,
            LocalDate checkOut,
            int guests,
            Pageable pageable
    );
}
