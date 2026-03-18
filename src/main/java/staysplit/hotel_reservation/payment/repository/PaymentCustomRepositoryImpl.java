package staysplit.hotel_reservation.payment.repository;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import staysplit.hotel_reservation.customer.domain.entity.QCustomerEntity;
import staysplit.hotel_reservation.payment.domain.entity.PaymentEntity;
import staysplit.hotel_reservation.payment.domain.entity.QPaymentEntity;
import staysplit.hotel_reservation.reservation.domain.entity.QReservationEntity;
import staysplit.hotel_reservation.reservation.domain.entity.QReservationParticipantEntity;

import java.util.List;

@RequiredArgsConstructor
public class PaymentCustomRepositoryImpl implements PaymentCustomRepository {

    private final JPAQueryFactory queryFactory;

    QPaymentEntity payment = QPaymentEntity.paymentEntity;
    QReservationParticipantEntity participant = QReservationParticipantEntity.reservationParticipantEntity;
    QReservationEntity reservation = QReservationEntity.reservationEntity;
    QCustomerEntity customer = QCustomerEntity.customerEntity;

    @Override
    public Page<PaymentEntity> findByCustomerId(Integer customerId, Pageable pageable) {
        List<PaymentEntity> paymentEntities = queryFactory
                .selectFrom(payment)
                .join(payment.reservationParticipant, participant).fetchJoin()
                .join(participant.reservation, reservation).fetchJoin()
                .join(participant.customer, customer).fetchJoin()
                .where(customer.id.eq(customerId))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(payment.count())
                .from(payment)
                .join(payment.reservationParticipant, participant)
                .join(participant.customer, customer)
                .where(customer.id.eq(customerId));

        return PageableExecutionUtils.getPage(paymentEntities, pageable, countQuery::fetchOne);
    }
}
