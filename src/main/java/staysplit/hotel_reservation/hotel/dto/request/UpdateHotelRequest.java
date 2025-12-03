package staysplit.hotel_reservation.hotel.dto.request;

import java.math.BigDecimal;

public record UpdateHotelRequest(
        String name,
        String address,
        Double longitude,
        Double latitude,
        String description,
        Integer starLevel
) {}