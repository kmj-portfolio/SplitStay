package staysplit.hotel_reservation.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import staysplit.hotel_reservation.payment.domain.entity.PaymentEntity;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long>, PaymentCustomRepository{

    Optional<PaymentEntity> findByPaymentId(String paymentId);
}