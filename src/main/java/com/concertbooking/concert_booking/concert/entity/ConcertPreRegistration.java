package com.concertbooking.concert_booking.concert.entity;
import com.concertbooking.concert_booking.user.entity.User;
import jakarta.persistence.*;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

//This will be for anti bot fair queue
@Entity
@Builder
@Getter
@Setter
@Table(name="concert_pre_registration",indexes = {
        @Index(name="idx_prereg_concert",columnList = "concert_id"),
        @Index(name="idx_prereg_user",columnList = "user_id"),

},uniqueConstraints = {
        @UniqueConstraint(name="uk_prereg_user_concert",
        columnNames = {"user_id","concert_id"})
})
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of="id")
@ToString(exclude = {"user","concert"})
public class ConcertPreRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id", nullable = false)
    private Concert concert;

    @Column(name = "queue_position")
    private Integer queuePosition;

    @Column(name = "purchase_window_start")
    private LocalDateTime purchaseWindowStart;

    @Column(name = "purchase_window_end")
    private LocalDateTime purchaseWindowEnd;

    @Column(name = "has_purchased", nullable = false)
    private boolean hasPurchased = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

