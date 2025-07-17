package staysplit.hotel_reservation.likeList.repository.likeListCustom;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import staysplit.hotel_reservation.customer.domain.entity.QCustomerEntity;
import staysplit.hotel_reservation.hotel.entity.QHotelEntity;
import staysplit.hotel_reservation.likeList.domain.entity.LikeListEntity;
import staysplit.hotel_reservation.likeList.domain.entity.QLikeListCustomer;
import staysplit.hotel_reservation.likeList.domain.entity.QLikeListEntity;
import staysplit.hotel_reservation.likeList.domain.entity.QLikeListHotelEntity;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class LikeListRepositoryImpl implements LikeListCustomRepository {

    private final JPAQueryFactory queryFactory;

    QLikeListEntity likeList = QLikeListEntity.likeListEntity;
    QLikeListCustomer participant = QLikeListCustomer.likeListCustomer;
    QLikeListHotelEntity likeListHotel = QLikeListHotelEntity.likeListHotelEntity;
    QHotelEntity hotel = QHotelEntity.hotelEntity;
    QCustomerEntity customer = QCustomerEntity.customerEntity;
    @Override
    public Optional<LikeListEntity> findByIdWithCustomersAndHotels(Integer likeListId) {
        LikeListEntity result = queryFactory
                .selectFrom(likeList)
                .leftJoin(likeList.hotels, likeListHotel)
                .leftJoin(likeListHotel.hotel, hotel)
                .leftJoin(likeList.participants, participant)
                .leftJoin(participant.customer, customer)
                .where(likeList.id.eq(likeListId))
                .distinct()
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public List<LikeListEntity> findByCustomerIdWithCustomersAndHotels(Integer customerId) {
        return queryFactory
                .selectFrom(likeList)
                .leftJoin(likeList.hotels, likeListHotel)
                .leftJoin(likeListHotel.hotel, hotel)
                .leftJoin(likeList.participants, participant)
                .leftJoin(participant.customer, customer)
                .where(customer.id.eq(customerId))
                .fetch();

    }
}
