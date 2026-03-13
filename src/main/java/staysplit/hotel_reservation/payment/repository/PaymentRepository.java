package staysplit.hotel_reservation.payment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import staysplit.hotel_reservation.payment.domain.entity.PaymentEntity;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {

    @EntityGraph(attributePaths = {"reservation"})
    Page<PaymentEntity> findByCustomerId(Integer customerId, Pageable pageable);

    boolean existsByPortOnePaymentId(String portOnePaymentId);

    Optional<PaymentEntity> findByPortOnePaymentId(String portOnePaymentId);
}