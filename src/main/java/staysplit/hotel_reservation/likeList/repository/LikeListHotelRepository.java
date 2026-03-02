package staysplit.hotel_reservation.likeList.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import staysplit.hotel_reservation.hotel.entity.HotelEntity;
import staysplit.hotel_reservation.likeList.domain.entity.LikeListEntity;
import staysplit.hotel_reservation.likeList.domain.entity.LikeListHotelEntity;

import java.util.Optional;

@Repository
public interface LikeListHotelRepository extends JpaRepository<LikeListHotelEntity, Integer> {


    @Query("SELECT lh" +
            " FROM LikeListHotelEntity lh" +
            " JOIN FETCH lh.likeList ll" +
            " JOIN FETCH lh.hotel h" +
            " WHERE lh.likeList = :likeList AND lh.hotel = :hotel")
    Optional<LikeListHotelEntity> findByLikeListAndHotel(@Param("likeList") LikeListEntity likeList,
                                                         @Param("hotel") HotelEntity hotel);
    boolean existsByLikeListAndHotel(LikeListEntity likeList, HotelEntity hotel);
}
