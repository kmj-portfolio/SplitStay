package staysplit.hotel_reservation.likes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import staysplit.hotel_reservation.likes.domain.entity.LikeListCustomer;

@Repository
public interface LikeListCustomerRepository extends JpaRepository<LikeListCustomer, Integer> {
}
