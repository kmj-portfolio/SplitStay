package staysplit.hotel_reservation.reservation.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import staysplit.hotel_reservation.hotel.entity.HotelEntity;
import staysplit.hotel_reservation.reservation.domain.enums.ReservationStatus;
import staysplit.hotel_reservation.reservedRoom.entity.ReservedRoomEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private HotelEntity hotel;

    @Builder.Default
    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReservedRoomEntity> reservedRooms = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReservationParticipantEntity> participants = new ArrayList<>();

    @Column(nullable = false, unique = true, length = 50)
    private String reservationNumber;

    @Column(nullable = false)
    private Integer nights;

    @Column(nullable = false)
    private LocalDate checkInDate;

    @Column(nullable = false)
    private LocalDate checkOutDate;

    private Long totalPrice;

    @Builder.Default
    @Column(nullable = false)
    private Long pricePaid = 0L;

    @Getter
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.WAITING_PAYMENT;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    public void addReservedRoom(ReservedRoomEntity reservedRoom) {
        reservedRooms.add(reservedRoom);
    }

    public void addParticipant(ReservationParticipantEntity participant) {
        participants.add(participant);
    }

    public void handleCancelledPayment(long amount) {
        this.pricePaid -= amount;

        if (this.status == ReservationStatus.CONFIRMED) {
            this.status = ReservationStatus.WAITING_PAYMENT;
        }
    }

    public void addPricePaid(long amount) {
        this.pricePaid += amount;
    }

    public void updateTotalPrice(Long totalPrice) {
        this.totalPrice = totalPrice;
    }

    public void markConfirmed() {
        this.status = ReservationStatus.CONFIRMED;
    }

    public void markCancelled() {
        this.status = ReservationStatus.CANCELLED;
    }

    public void markExpired() {
        this.status = ReservationStatus.EXPIRED;
    }

    public boolean isComplete() {
        return this.status == ReservationStatus.COMPLETE;
    }
}
