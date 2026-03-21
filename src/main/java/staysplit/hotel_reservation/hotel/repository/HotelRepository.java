package staysplit.hotel_reservation.hotel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import staysplit.hotel_reservation.hotel.entity.HotelEntity;

import java.util.Optional;

@Repository
public interface HotelRepository extends JpaRepository<HotelEntity, Integer> {
    Optional<HotelEntity> findByName(String name);
}
