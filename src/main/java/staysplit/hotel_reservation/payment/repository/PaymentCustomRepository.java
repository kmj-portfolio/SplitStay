package staysplit.hotel_reservation.payment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import staysplit.hotel_reservation.payment.domain.entity.PaymentEntity;

public interface PaymentCustomRepository {

    // Fetch Join으로 N + 1 문제 방지
    Page<PaymentEntity> findByCustomerId(Integer customerId, Pageable pageable);
}
