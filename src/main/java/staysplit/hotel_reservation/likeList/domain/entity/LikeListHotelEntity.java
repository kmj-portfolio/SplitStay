package staysplit.hotel_reservation.likeList.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import staysplit.hotel_reservation.hotel.entity.HotelEntity;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class LikeListHotelEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "like_list_hotel_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "like_list_id", nullable = false)
    private LikeListEntity likeList;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private HotelEntity hotel;
}
