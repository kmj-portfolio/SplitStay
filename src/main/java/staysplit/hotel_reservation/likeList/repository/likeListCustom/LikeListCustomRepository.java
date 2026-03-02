package staysplit.hotel_reservation.likeList.repository.likeListCustom;

import org.springframework.stereotype.Repository;
import staysplit.hotel_reservation.likeList.domain.entity.LikeListEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface LikeListCustomRepository {
    Optional<LikeListEntity> findByIdWithCustomersAndHotels(Integer likeListId);

    List<LikeListEntity> findByCustomerIdWithCustomersAndHotels(Integer customerId);

}
