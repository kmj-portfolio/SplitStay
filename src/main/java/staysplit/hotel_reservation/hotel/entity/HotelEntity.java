package staysplit.hotel_reservation.hotel.entity;

import jakarta.persistence.*;
import lombok.*;
import staysplit.hotel_reservation.hotel.dto.request.UpdateHotelRequest;
import staysplit.hotel_reservation.photo.domain.PhotoEntity;
import staysplit.hotel_reservation.provider.domain.entity.ProviderEntity;

import java.math.BigDecimal;
import java.util.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class HotelEntity {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Column(name = "hotel_id")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private ProviderEntity provider;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false, precision = 17, scale = 14)
    private BigDecimal longitude;

    @Column(nullable = false, precision = 17, scale = 14)
    private BigDecimal latitude;

    private String description;

    private Integer starLevel;

    @Builder.Default
    private Double rating = 0.0;

    @Builder.Default
    private Integer reviewCount = 0;

    @Builder.Default
    @OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PhotoEntity> photos = new ArrayList<>();

    public void updateHotel(UpdateHotelRequest request) {
        this.name = request.name();
        this.address = request.address();
        this.longitude = request.longitude();
        this.latitude = request.latitude();
        this.description = request.description();
        this.starLevel = request.starLevel();
    }

    public void addPhoto(PhotoEntity photo) {
        photos.add(photo);
        photo.setHotel(this);
    }

    public Optional<PhotoEntity> getMainPhoto() {
        return photos.stream()
                .filter(PhotoEntity::isMainPhoto)
                .findFirst();
    }
}