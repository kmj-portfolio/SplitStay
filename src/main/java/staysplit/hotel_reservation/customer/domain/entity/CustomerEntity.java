package staysplit.hotel_reservation.customer.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import staysplit.hotel_reservation.customer.domain.dto.SocialType;
import staysplit.hotel_reservation.user.domain.entity.UserEntity;

import java.time.LocalDate;

@Entity
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerEntity {

    @Id
    @Column(name = "customer_id")
    @Getter
    private Integer id;

    @MapsId
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate birthdate;

    @Column(length = 11, nullable = false)
    private String phoneNumber;

    @Setter
    @Column(length = 30, unique = true)
    private String nickname;

    @Enumerated(EnumType.STRING)
    private SocialType socialType;

    private String socialId;
}
