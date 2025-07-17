package staysplit.hotel_reservation.likeList.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import staysplit.hotel_reservation.customer.domain.entity.CustomerEntity;
import staysplit.hotel_reservation.likeList.domain.entity.LikeListCustomer;
import staysplit.hotel_reservation.likeList.domain.entity.LikeListEntity;

@Repository
public interface LikeListCustomerRepository extends JpaRepository<LikeListCustomer, Integer> {
    boolean existsByLikeListAndCustomer(LikeListEntity likeList, CustomerEntity customer);
}
