package staysplit.hotel_reservation.customer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import staysplit.hotel_reservation.customer.domain.entity.CustomerEntity;
import staysplit.hotel_reservation.user.domain.entity.UserEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, Integer> {
    Optional<CustomerEntity> findByUser(UserEntity user);
    boolean existsByNickname(String nickname);

    Optional<CustomerEntity> findByUserEmail(String email);

    @Query("SELECT c.nickname FROM CustomerEntity c")
    List<String> findAllUsernames();

    Optional<CustomerEntity> findByNickname(String nickname);
}
