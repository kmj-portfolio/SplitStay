package staysplit.hotel_reservation.reservation.reposiotry.search;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;
import staysplit.hotel_reservation.customer.domain.entity.QCustomerEntity;
import staysplit.hotel_reservation.hotel.entity.QHotelEntity;
import staysplit.hotel_reservation.reservation.domain.entity.QReservationEntity;
import staysplit.hotel_reservation.reservation.domain.entity.QReservationParticipantEntity;
import staysplit.hotel_reservation.reservation.domain.entity.ReservationEntity;
import staysplit.hotel_reservation.reservation.domain.enums.ReservationStatus;
import staysplit.hotel_reservation.reservedRoom.entity.QReservedRoomEntity;
import staysplit.hotel_reservation.room.domain.QRoomEntity;
import staysplit.hotel_reservation.user.domain.entity.QUserEntity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class ReservationRepositoryImpl implements ReservationRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    QReservationEntity reservation = QReservationEntity.reservationEntity;
    QHotelEntity hotel = QHotelEntity.hotelEntity;
    QReservedRoomEntity reservedRoom = QReservedRoomEntity.reservedRoomEntity;
    QRoomEntity room = QRoomEntity.roomEntity;

    QReservationParticipantEntity participant = QReservationParticipantEntity.reservationParticipantEntity;
    QCustomerEntity customer = QCustomerEntity.customerEntity;
    QUserEntity user = QUserEntity.userEntity;


    // customer id, Reservation Status, 특정 Local Date 이후로 검색 가능
    @Override
    public Page<ReservationEntity> findReservationsByCustomerWithFilters(Integer customerId,
                                                                          ReservationStatus status,
                                                                          LocalDate afterDate,Pageable pageable) {
        QReservationEntity reservation = QReservationEntity.reservationEntity;
        QReservationParticipantEntity participant = QReservationParticipantEntity.reservationParticipantEntity;

        BooleanBuilder booleanBuilder = new BooleanBuilder();
        booleanBuilder.and(participant.customer.id.eq(customerId));

        if (status != null) {
            booleanBuilder.and(reservation.status.eq(status));
        }

        if (afterDate != null) {
            booleanBuilder.and(reservation.checkInDate.gt(afterDate));
        }

        List<ReservationEntity> results = queryFactory
                .selectFrom(reservation)
                .distinct()
                .join(reservation.participants, participant)
                .where(booleanBuilder)
                .orderBy(reservation.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(reservation.countDistinct())
                .from(reservation)
                .join(reservation.participants, participant)
                .where(booleanBuilder)
                .fetchOne();

        return new PageImpl<>(results, pageable, total != null ? total : 0);
    }

    @Override
    public Page<ReservationEntity> findReservationsByHotelWithFilters(Integer hotelId, Pageable pageable,
                                                                      ReservationSearchConditionForProviders condition) {
        // content 쿼리
        List<ReservationEntity> content = queryFactory
                .select(reservation)
                .distinct()
                .from(reservation)
                .join(reservation.hotel, hotel).fetchJoin()
                .join(reservation.reservedRooms, reservedRoom) // list
                .join(reservedRoom.room, room)
                .join(reservation.participants, participant) // list
                .join(participant.customer, customer)
                .join(customer.user, user)
                .where(hotel.id.eq(hotelId)
                        .and(reservationNumberEq(condition.reservationNumber()))
                        .and(statusEq(condition.reservationStatus()))
                        .and(participantEmailEq(condition.guestEmail()))
                        .and(participantNameEq(condition.guestName()))
                        .and(roomTypeEq(condition.roomType()))
                        .and(checkInDateRange(condition.checkInStart(), condition.checkInEnd()))
                        .and(checkOutDateRange(condition.checkOutStart(), condition.checkOutEnd()))
                        .and(createdAtDateRange(condition.createdAtStart(), condition.createdAtEnd())))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // count 쿼리
        Long total = queryFactory
                .select(reservation.countDistinct())
                .from(reservation)
                .join(reservation.hotel, hotel)
                .join(reservation.reservedRooms, reservedRoom) // list
                .join(reservedRoom.room, room)// many to one
                .join(reservation.participants, participant) // list
                .join(participant.customer, customer)
                .join(customer.user, user)
                .where(reservation.hotel.id.eq(hotelId)
                        .and(reservationNumberEq(condition.reservationNumber()))
                        .and(statusEq(condition.reservationStatus()))
                        .and(participantEmailEq(condition.guestEmail()))
                        .and(participantNameEq(condition.guestName()))
                        .and(roomTypeEq(condition.roomType()))
                        .and(checkInDateRange(condition.checkInStart(), condition.checkInEnd()))
                        .and(checkOutDateRange(condition.checkOutStart(), condition.checkOutEnd()))
                        .and(createdAtDateRange(condition.createdAtStart(), condition.createdAtEnd())))
                .fetchOne();


        return new PageImpl<>(content, pageable, total != null ? total : 0);
     }

    private BooleanExpression reservationNumberEq(String reservationNumber) {
        return StringUtils.hasText(reservationNumber) ? reservation.reservationNumber.eq(reservationNumber) : null;
    }

    private BooleanExpression statusEq(String statusStr) {
        if (!StringUtils.hasText(statusStr)) {
            return null;
        }

        Optional<ReservationStatus> status = ReservationStatus.from(statusStr);
        return status.isPresent() ? reservation.status.eq(status.get()) : null;
    }

    private BooleanExpression participantEmailEq(String email) {
        return StringUtils.hasText(email) ? user.email.eq(email) : null;
    }

    private BooleanExpression participantNameEq(String name) {
        return StringUtils.hasText(name) ? customer.name.eq(name) : null;
    }

    private BooleanExpression roomTypeEq(String roomType) {
        return StringUtils.hasText(roomType) ? room.roomType.eq(roomType) : null;
    }

    private BooleanExpression checkInDateRange(LocalDate checkInStart, LocalDate checkInEnd) {
        if (checkInStart != null && checkInEnd != null) {
            return reservation.checkInDate.goe(checkInStart).and(reservation.checkInDate.loe(checkInEnd));
        }

        if (checkInStart != null) {
            return reservation.checkInDate.goe(checkInStart);
        }

        if (checkInEnd != null) {
            return reservation.checkInDate.loe(checkInEnd);
        }

        return null;
    }

    private BooleanExpression checkOutDateRange(LocalDate start, LocalDate end) {
        if (start != null && end != null) {
            return reservation.checkOutDate.goe(start).and(reservation.checkOutDate.loe(end));
        }

        if (start != null) {
            return reservation.checkOutDate.goe(start);
        }

        if (end != null) {
            return reservation.checkOutDate.loe(end);
        }

        return null;
    }

    private BooleanExpression createdAtDateRange(LocalDate start, LocalDate end) {
        if (start != null && end != null) {
            return reservation.createdAt.goe(start.atStartOfDay()).and(reservation.createdAt.loe(end.atTime(LocalTime.MAX)));
        }

        if (start != null) {
            return reservation.createdAt.goe(start.atStartOfDay());
        }

        if (end != null) {
            return reservation.createdAt.loe(end.atTime(LocalTime.MAX));
        }

        return null;
    }
}
