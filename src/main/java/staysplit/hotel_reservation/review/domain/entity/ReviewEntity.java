package staysplit.hotel_reservation.review.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import staysplit.hotel_reservation.customer.domain.entity.CustomerEntity;
import staysplit.hotel_reservation.hotel.entity.HotelEntity;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter(AccessLevel.PACKAGE)
@Builder
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    CustomerEntity customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    HotelEntity hotel;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private Integer rating;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime modifiedAt;

    private LocalDateTime deletedAt;

    public void setContent(String content) {
        this.content = content;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public Integer geCustomerId() {
        return customer != null ? customer.getId() : null;
    }

    public Integer getHotelId() {
        return hotel != null ? hotel.getId() : null;
    }

    public String getNickname() {
        return customer != null ? customer.getNickname() : null;
    }

    public void markDeleted() {
        this.deletedAt = LocalDateTime.now();
    }

}
