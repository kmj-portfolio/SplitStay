package staysplit.hotel_reservation.likeList.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import staysplit.hotel_reservation.customer.domain.entity.CustomerEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class LikeListEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "like_list_id")
    private Integer id;

    @Column(nullable = false)
    private String listName;

    @OneToOne
    private CustomerEntity owner;

    @Builder.Default
    @OneToMany(mappedBy = "likeList", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LikeListCustomer> participants = new ArrayList<>();
    @Builder.Default
    @OneToMany(mappedBy = "likeList", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LikeListHotelEntity> hotels = new ArrayList<>();

    public void changeListName(String listName) {
        this.listName = listName;
    }
}
