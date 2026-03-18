package staysplit.hotel_reservation.payment.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import staysplit.hotel_reservation.payment.domain.enums.PaymentStatus;
import staysplit.hotel_reservation.reservation.domain.entity.ReservationParticipantEntity;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_participant_id", nullable = false)
    ReservationParticipantEntity reservationParticipant;

    @Column(unique = true, nullable = false)
    private String paymentId;

    @Getter
    @Column(nullable = false)
    private Long amount;

    private String payMethod;

    private String cardPublisherName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @CreatedDate
    @Column(name = "paid_at", updatable = false)
    private LocalDateTime paidAt;

    public boolean isPaid() {
        return status == PaymentStatus.PAID;
    }

    public boolean isCancelled() {
        return status == PaymentStatus.CANCELLED;
    }

    public void markPaid() {
        this.status = PaymentStatus.PAID;
    }

    public void markCancelled() {
        this.status = PaymentStatus.CANCELLED;
    }

}
