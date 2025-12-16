package staysplit.hotel_reservation.hotelSearch.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import staysplit.hotel_reservation.hotel.entity.HotelEntity;
import staysplit.hotel_reservation.hotel.entity.QHotelEntity;
import staysplit.hotel_reservation.hotelSearch.dto.request.HotelSearchCondition;
import staysplit.hotel_reservation.reservation.domain.entity.QReservationEntity;
import staysplit.hotel_reservation.reservedRoom.entity.QReservedRoomEntity;
import staysplit.hotel_reservation.room.domain.QRoomEntity;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class HotelSearchRepositoryIml implements HotelSearchRepository {

    private final JPAQueryFactory queryFactory;

    private static final double SEARCH_RADIUS = 5.0; // 검색할 반경 (km)

    private static final QHotelEntity HOTEL = QHotelEntity.hotelEntity;
    private static final QRoomEntity ROOM = QRoomEntity.roomEntity;

    @Override
    public Page<HotelEntity> searchNearbyHotels(HotelSearchCondition condition, Pageable pageable) {
        // 체크인, 체크아웃 날짜, 인원, 가격, 별점 계산
        BooleanBuilder builder = buildSearchPredicate(condition);

        // 거리 계산
        NumberExpression<Double> distance = calculateDistance(condition.getLongitude(), condition.getLatitude());
        builder.and(distance.loe(SEARCH_RADIUS));

        // Radius 안의 호텔 검색
        List<Integer> hotelIdsWithinDistance = queryFactory
                .select(HOTEL.id)
                .from(HOTEL)
                .join(ROOM).on(ROOM.hotel.eq(HOTEL))
                .where(builder)
                .groupBy(HOTEL.id)       // 호텔 단위로 묶어서 중복 제거
                .orderBy(distance.asc()) // 거리 순 정렬
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(HOTEL.id.countDistinct())
                .from(HOTEL)
                .join(ROOM).on(ROOM.hotel.eq(HOTEL))
                .where(builder)
                .fetchOne();

        // hotel 20개만 상세조회
        List<HotelEntity> content = queryFactory
                .selectFrom(HOTEL)
                .where(HOTEL.id.in(hotelIdsWithinDistance))
                .fetch();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    // 조건 조합: 체크인, 체크아웃 날짜, 인원, 가격, 별점 계산  (거리는 없음)
    private BooleanBuilder buildSearchPredicate(HotelSearchCondition condition) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(boundingBox(condition.getLongitude(), condition.getLatitude()));    // bounding box
        builder.and(availableBetween(condition.getCheckIn(), condition.getCheckOut())); // overlap
        builder.and(numGuestGoe(condition.getNumGuest()));
        builder.and(minPrice(condition.getMinPrice()));
        builder.and(maxPrice(condition.getMaxPrice()));
        builder.and(starLevelGoe(condition.getNumStar()));
        return builder;
    }

    // MySQL ST_Distance_Sphere 사용해서 두 coordinate 거리 구하기 (KM)
    private NumberExpression calculateDistance(Double givenLongitude, Double givenLatitude) {

        NumberExpression<Double> distanceMeter = Expressions.numberTemplate(
                Double.class,
                "ST_Distance_Sphere(POINT({0}, {1}), POINT({2}, {3}))",
                HOTEL.longitude,            // 호텔의 경도와 위도
                HOTEL.latitude,
                givenLongitude,  // 사용자가 입력한 위치의 경도와 위도
                givenLatitude
        );

        // 두 지점간의 거리를 m에서 km로 변환
        return distanceMeter.divide(1000.0);
    }

    // Bounding Box
    public BooleanExpression boundingBox(Double givenLongitude, Double givenLatitude) {
        double latDelta = SEARCH_RADIUS / 110.0;
        double lonDelta = SEARCH_RADIUS / (110.0 * Math.cos(Math.toRadians(givenLatitude)));

        double minLat = givenLatitude - latDelta;
        double maxLat = givenLatitude + latDelta;
        double minLon = givenLongitude - lonDelta;
        double maxLon = givenLongitude + lonDelta;

        return HOTEL.latitude.between(minLat, maxLat).and(
                HOTEL.longitude.between(minLon, maxLon));
    }

    // 주어진 날짜 구간에 겹치는 예약이 없는 ROOM만 true
    private BooleanExpression availableBetween(LocalDate checkIn, LocalDate checkOut) {
        QReservationEntity reservationSub = new QReservationEntity("reservationSub");
        QReservedRoomEntity reservedRoomSub = new QReservedRoomEntity("reservedRoomSub");

        // 해당 ROOM에 대해 주어진 기간과 겹치는 총 예약수량을 구하는 SubQUery
        JPQLQuery<Integer> reservedQty = JPAExpressions
                .select(reservedRoomSub.quantity.sum().coalesce(0))
                .from(reservedRoomSub)
                .join(reservedRoomSub.reservation, reservationSub)
                .where(reservedRoomSub.room.eq(ROOM),
                        reservationSub.checkInDate.lt(checkOut),
                        reservationSub.checkOutDate.gt(checkIn)
                );

        return ROOM.totalQuantity.gt(reservedQty);

    }

    private BooleanExpression numGuestGoe(Integer numGuest) {
        return numGuest == null ? null : ROOM.maxOccupancy.goe(numGuest);
    }

    private BooleanExpression minPrice(Integer minPrice) {
        return minPrice == null ? null : ROOM.price.goe(minPrice);
    }

    private BooleanExpression maxPrice(Integer maxPrice) {
        return maxPrice == null ? null : ROOM.price.loe(maxPrice);
    }

    private BooleanExpression starLevelGoe(Integer numStar) {
        return numStar == null ? null : HOTEL.starLevel.goe(numStar);
    }

}
