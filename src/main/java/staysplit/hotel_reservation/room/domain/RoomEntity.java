package staysplit.hotel_reservation.room.domain;

import jakarta.persistence.*;
import lombok.*;
import staysplit.hotel_reservation.hotel.entity.HotelEntity;
import staysplit.hotel_reservation.photo.domain.PhotoEntity;

import java.util.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RoomEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private HotelEntity hotel;

    @Column(nullable = false)
    private String roomType;

    @Column( nullable = false)
    private Integer maxOccupancy;

    @Column(nullable = false)
    private Integer price;

    private String description;

    @Column(nullable = false)
    private Integer totalQuantity;

    @Builder.Default
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PhotoEntity> photos = new ArrayList<>();

    public void updateRoom(String roomType, Integer maxOccupancy,
                           Integer price, String description, Integer totalQuantity) {
        this.roomType = roomType;
        this.maxOccupancy = maxOccupancy;
        this.price = price;
        this.description = description;
        this.totalQuantity = totalQuantity;
    }

    public void addPhoto(PhotoEntity photo) {
        photos.add(photo);
        photo.setRoom(this);
    }

    public Optional<PhotoEntity> getMainPhoto() {
        return photos.stream()
                .filter(PhotoEntity::isMainPhoto)
                .findFirst();
    }
}