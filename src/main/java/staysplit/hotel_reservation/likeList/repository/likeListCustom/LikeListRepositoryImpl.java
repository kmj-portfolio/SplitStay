package staysplit.hotel_reservation.likeList.repository.likeListCustom;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import staysplit.hotel_reservation.customer.domain.entity.QCustomerEntity;
import staysplit.hotel_reservation.likeList.domain.entity.LikeListEntity;
import staysplit.hotel_reservation.likeList.domain.entity.QLikeListCustomer;
import staysplit.hotel_reservation.likeList.domain.entity.QLikeListEntity;

import java.util.List;

@RequiredArgsConstructor
public class LikeListRepositoryImpl implements LikeListCustomRepository {

    private final JPAQueryFactory queryFactory;

    QLikeListEntity likeList = QLikeListEntity.likeListEntity;
    QCustomerEntity customer = QCustomerEntity.customerEntity;
    QLikeListCustomer participant = QLikeListCustomer.likeListCustomer;

    @Override
    public Page<LikeListEntity> findByCustomerIdAsOwnerAndParticipant(Integer customerId, Pageable pageable) {
         List<LikeListEntity> content = queryFactory
                .selectFrom(likeList)
                .leftJoin(likeList.owner, customer).fetchJoin()
                 .leftJoin(likeList.participants, participant)
                .where(
                        customer.id.eq(customerId)
                                .or(participant.customer.id.eq(customerId)))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

         Long total = queryFactory
                 .select(likeList.count())
                 .from(likeList)
                 .leftJoin(likeList.owner, customer)
                 .leftJoin(likeList.participants, participant)
                 .where(
                         customer.id.eq(customerId)
                         .or(participant.id.eq(customerId)))
                 .fetchOne();

         return new PageImpl<>(content, pageable, total);
    }
}
