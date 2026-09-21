package staysplit.hotel_reservation.room.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import staysplit.hotel_reservation.room.domain.RoomEntity;

import java.util.List;
import java.util.Optional;


@Repository
public interface RoomRepository extends JpaRepository<RoomEntity, Integer> {
    Page<RoomEntity> findByHotelId(Integer hotelId, Pageable pageable);
    List<RoomEntity> findByHotelId(Integer hotelId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RoomEntity r WHERE r.id = :id")
    Optional<RoomEntity> findByIdWithLock(Integer id);

    @Query("""
            SELECT r.hotel.id AS hotelId, MIN(r.price) AS minPrice
            FROM RoomEntity r
            WHERE r.hotel.id IN :hotelIds
            GROUP BY r.hotel.id
        """)
    List<HotelMinPrice> findMinPriceByHotelIds(@Param("hotelIds") List<Integer> hotelIds);

    interface HotelMinPrice {
        Integer getHotelId();
        Integer getMinPrice();
    }
}

