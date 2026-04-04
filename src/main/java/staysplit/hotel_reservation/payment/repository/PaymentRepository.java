package staysplit.hotel_reservation.payment.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import staysplit.hotel_reservation.payment.domain.entity.PaymentEntity;
import staysplit.hotel_reservation.payment.domain.enums.PaymentStatus;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long>, PaymentCustomRepository{

    Optional<PaymentEntity> findByPaymentId(String paymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PaymentEntity p where p.paymentId = :paymentId")
    Optional<PaymentEntity> findByPaymentIdWithLock(String paymentId);

    boolean existsByReservationParticipantIdAndStatusIn(Integer participantId, List<PaymentStatus> paymentStatusList);

}