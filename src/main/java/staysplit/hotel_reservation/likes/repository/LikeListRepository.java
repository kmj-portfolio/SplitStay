package staysplit.hotel_reservation.likes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import staysplit.hotel_reservation.likes.domain.entity.LikeListEntity;

@Repository
public interface LikeListRepository extends JpaRepository<LikeListEntity, Integer> {

}
