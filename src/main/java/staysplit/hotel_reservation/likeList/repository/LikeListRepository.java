package staysplit.hotel_reservation.likeList.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import staysplit.hotel_reservation.likeList.domain.entity.LikeListEntity;
import staysplit.hotel_reservation.likeList.repository.likeListCustom.LikeListCustomRepository;

import java.util.Optional;

@Repository
public interface LikeListRepository extends JpaRepository<LikeListEntity, Integer>, LikeListCustomRepository {

    @Query("SELECT ll" +
            " FROM LikeListEntity ll" +
            " JOIN ll.participants p" +
            " JOIN p.customer" +
            " WHERE ll.id = :id")
    Optional<LikeListEntity> findByIdWithCustomers(@Param("id") Integer id);

}
