package staysplit.hotel_reservation.room.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import staysplit.hotel_reservation.room.domain.RoomEntity;

import java.util.Optional;


@Repository
public interface RoomRepository extends JpaRepository<RoomEntity, Integer> {
    Page<RoomEntity> findByHotel_Id(Integer hotelId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RoomEntity r WHERE r.id = :id")
    Optional<RoomEntity> findByIdWithLock(Integer id);
}

