package staysplit.hotel_reservation.hotel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import staysplit.hotel_reservation.hotel.entity.HotelEntity;

@Repository
public interface HotelRepository extends JpaRepository<HotelEntity, Integer> {

}
