package staysplit.hotel_reservation.hotelSearch.repository;

import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import staysplit.hotel_reservation.hotel.entity.HotelEntity;
import staysplit.hotel_reservation.hotel.entity.QHotelEntity;
import staysplit.hotel_reservation.hotelSearch.dto.request.HotelSearchCondition;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class HotelSearchRepositoryIml implements HotelSearchRepository {
    private final JPAQueryFactory queryFactory;

    @Override
    public Page<HotelEntity> searchNearbyHotels(HotelSearchCondition condition, Pageable pageable) {
        QHotelEntity hotel = QHotelEntity.hotelEntity;

        // 1. 중심 latitude와 latitude를 radians로 계산)
        NumberExpression<Double> centerLatRad =
                Expressions.numberTemplate(Double.class, "RADIANS({0})", condition.getLatitude());
        NumberExpression<Double> centerLonRad =
                Expressions.numberTemplate(Double.class, "RADIANS({0})", condition.getLongitude());

        // 호텔의 longitude와 latitude도 radians로 변환
        NumberExpression<Double> hotelLatRad =
                Expressions.numberTemplate(Double.class, "RADIANS({0})", hotel.latitude);
        NumberExpression<Double> hotelLonRad =
                Expressions.numberTemplate(Double.class, "RADIANS({0})", hotel.longitude);

        // 2. 두 지점의 위도와 경도 차이 계산
        NumberExpression<Double> deltaLat = hotelLatRad.subtract(centerLatRad);
        NumberExpression<Double> deltaLon = hotelLonRad.subtract(centerLonRad);

        // 3.
        NumberExpression<Double> sinDeltaLatHalf =
                Expressions.numberTemplate(Double.class, "SIN(({0}) / 2)", deltaLat);
        NumberExpression<Double> sinDeltaLonHalf =
                Expressions.numberTemplate(Double.class, "SIN(({0}) / 2)", deltaLon);

        NumberExpression<Double> squaredLat = sinDeltaLatHalf.multiply(sinDeltaLatHalf);
        NumberExpression<Double> squaredLon = sinDeltaLonHalf.multiply(sinDeltaLonHalf);

        // cos(lat1), cos(lat2)
        NumberExpression<Double> cosLat1 =
                Expressions.numberTemplate(Double.class, "COS({0})", centerLatRad);
        NumberExpression<Double> cosLat2 =
                Expressions.numberTemplate(Double.class, "COS({0})", hotelLatRad);

        // Haversine의 a계산
        NumberExpression<Double> cosPart = cosLat1.multiply(cosLat2).multiply(squaredLon);
        NumberExpression<Double> a = squaredLat.add(cosPart);

        // Haversine의 c 계산
        NumberExpression<Double> sqrtA =
                Expressions.numberTemplate(Double.class, "SQRT({0})", a);
        NumberExpression<Double> c =
                Expressions.numberTemplate(Double.class, "2 * ASIN({0})", sqrtA);

        // 최종 거리 계산 = R * c
        NumberExpression<Double> distance =
                Expressions.numberTemplate(Double.class, "{0} * {1}", 6371.0, c);

        List<HotelEntity> content = queryFactory
                .selectFrom(hotel)
                .where(distance.loe(5.0))   // 5km 이내
                .orderBy(distance.asc())    // 거리순
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(hotel.count())
                .from(hotel)
                .where(distance.loe(5.0))
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }



}
