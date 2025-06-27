package staysplit.hotel_reservation.likes.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import staysplit.hotel_reservation.customer.domain.entity.CustomerEntity;

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

    @OneToOne
    private CustomerEntity owner;

    @OneToMany(mappedBy = "likeList", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LikeListCustomer> customers;

    @Column(nullable = false)
    private String listName;

}
