package staysplit.hotel_reservation.likeList.repository.likeListCustom;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import staysplit.hotel_reservation.likeList.domain.entity.LikeListEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface LikeListCustomRepository {

    Page<LikeListEntity> findByCustomerIdAsOwnerAndParticipant(Integer customerId, Pageable pageable);
}
