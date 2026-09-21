package staysplit.hotel_reservation.hotelSearch.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;
import org.springframework.util.ObjectUtils;
import staysplit.hotel_reservation.hotel.entity.HotelEntity;
import staysplit.hotel_reservation.hotel.entity.QHotelEntity;
import staysplit.hotel_reservation.hotelSearch.dto.request.HotelSearchCondition;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class HotelSearchRepositoryIml implements HotelSearchRepository {

    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;

    private static final double SEARCH_RADIUS_METERS = 5_000.0;

    private static final double SEARCH_RADIUS_KM = 5.0;

    private static final QHotelEntity HOTEL = QHotelEntity.hotelEntity;

    @Override
    public Slice<HotelEntity> searchNearbyHotels(
            HotelSearchCondition condition,
            Pageable pageable
    ) {

        /*
            1차 조회 - 거리 순으로 호텔 ID만 조회

            - Slice의 hasNext 확인을 위해 pageSize + 1개를 가져온다

            - ST_Distance_Sphere()를 WHERE와 ORDER BY에서 각각 호출하면
              옵티마이저가 값을 재사용하지 않기 때문에 MySQL이 두번 계산해서 비용이 두 배가 된다.

                   => 서브쿼리로 한 번만 계산해서 dist 컬럼으로 재사용한다.
         */
        List<Integer> hotelIdsWithinDistance = fetchHotelIdsWithinDistance(condition, pageable);

        boolean hasNext =
                hotelIdsWithinDistance.size() > pageable.getPageSize();

        List<Integer> pagedHotelIds = hasNext
                ? hotelIdsWithinDistance.subList(
                0,
                pageable.getPageSize()
        )
                : hotelIdsWithinDistance;

        // 결과가 없다면 바로 빈 Slice 반환
        if (pagedHotelIds.isEmpty()) {
            return new SliceImpl<>(
                    List.of(),
                    pageable,
                    false
            );
        }


        // 2차 조회 - ID 기분으로 HotelEntity 상세 조회
        List<HotelEntity> content = queryFactory
                .selectFrom(HOTEL)
                .where(HOTEL.id.in(pagedHotelIds))
                .fetch();

        // IN 절은 ID 순서를 보장하지 않기 때문에, 1차 조회의 거리 순으로 다시 정렬
        content.sort(
                (a, b) -> Integer.compare(
                        pagedHotelIds.indexOf(a.getId()),
                        pagedHotelIds.indexOf(b.getId())
                )
        );

        return new SliceImpl<>(
                content,
                pageable,
                hasNext
        );
    }

    /*
        거리 순으로 정렬된 호텔 ID 목록을 조회

        ST_Distance_Sphere()는 subquery 안에서 dist 컬럼으로 1번만 계산하고, 바깥 쿼리에서 그 컬럼을 필터/정렬에 재사용한다.
     */
    private List<Integer> fetchHotelIdsWithinDistance(HotelSearchCondition condition, Pageable pageable) {

        double latDelta = SEARCH_RADIUS_KM / 110.0;

        double lonDelta = SEARCH_RADIUS_KM / (110.0 * Math.cos(Math.toRadians(condition.latitude())));

        double minLat = condition.latitude() - latDelta;
        double maxLat = condition.latitude() + latDelta;
        double minLon = condition.longitude() - lonDelta;
        double maxLon = condition.longitude() + lonDelta;
        
        StringBuilder sql = new StringBuilder(
                "SELECT t.hotel_id FROM ( "
                        + "    SELECT h.hotel_id, "
                        + "           ST_Distance_Sphere(POINT(h.longitude, h.latitude), POINT(:lon, :lat)) AS dist "
                        + "    FROM hotel_entity h "
                        + "    WHERE h.latitude BETWEEN :minLat AND :maxLat "
                        + "      AND h.longitude BETWEEN :minLon AND :maxLon "
        );

        sql.append(
                "      AND EXISTS ( "
                        + "          SELECT 1 FROM room_entity r "
                        + "          WHERE r.hotel_id = h.hotel_id "
                        + "            AND r.total_quantity > ( "
                        + "                SELECT COALESCE(SUM(rr.quantity), 0) "
                        + "                FROM reserved_room_entity rr "
                        + "                JOIN reservation_entity res ON rr.reservation_id = res.reservation_id "
                        + "                WHERE rr.room_id = r.room_id "
                        + "                  AND res.check_in_date < :checkOut "
                        + "                  AND res.check_out_date > :checkIn "
                        + "            ) "
        );

        boolean hasStarFilter = !ObjectUtils.isEmpty(condition.numStar());
        if (hasStarFilter) {
            sql.append(" AND h.star_level IN (:numStars) ");
        }

        if (condition.numGuest() != null) {
            sql.append(" AND r.max_occupancy >= :numGuest ");
        }

        if (condition.minPrice() != null) {
            sql.append(" AND r.price >= :minPrice ");
        }

        if (condition.maxPrice() != null) {
            sql.append(" AND r.price <= :maxPrice ");
        }

        sql.append("      ) " // EXISTS( 닫는 괄호
                        + ") t " // FROM( 닫는 괄호
                        + "WHERE t.dist <= :radius "
                        + "ORDER BY t.dist ASC "
                        + "LIMIT :limit OFFSET :offset"
        );

        Query query = entityManager.createNativeQuery(sql.toString())
                .setParameter("lon", condition.longitude())
                .setParameter("lat", condition.latitude())
                .setParameter("minLat", minLat)
                .setParameter("maxLat", maxLat)
                .setParameter("minLon", minLon)
                .setParameter("maxLon", maxLon)
                .setParameter("radius", SEARCH_RADIUS_METERS)
                .setParameter("checkIn", condition.checkIn())
                .setParameter("checkOut", condition.checkOut())
                .setParameter("limit", pageable.getPageSize() + 1L)
                .setParameter("offset", pageable.getOffset());

        if (hasStarFilter) {
            query.setParameter("numStars", condition.numStar());
        }

        if (condition.numGuest() != null) {
            query.setParameter("numGuest", condition.numGuest());
        }

        if (condition.minPrice() != null) {
            query.setParameter("minPrice", condition.minPrice());
        }

        if (condition.maxPrice() != null) {
            query.setParameter("maxPrice", condition.maxPrice());
        }

        @SuppressWarnings("unchecked")
        List<Number> rows = query.getResultList();

        return rows.stream()
                .map(Number::intValue)
                .toList();
    }
}
