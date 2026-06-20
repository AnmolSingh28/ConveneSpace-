package com.concertbooking.concert_booking.seat.entity;

import com.concertbooking.concert_booking.common.enums.SeatStatus;
import com.concertbooking.concert_booking.concert.entity.TicketTier;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name="seat_inventory",indexes = {
        @Index(name="idx_seat_inventory",columnList = "tier_id,id"),
        @Index(name="idx_seat_status",columnList = "status")
})
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of="id")
public class SeatInventory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tier_id", nullable = false)
    private TicketTier tier;

    @Column(name = "row_label")
    private String rowLabel;

    @Column(name = "seat_number")
    private String seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locked_by_user_id")
    private com.concertbooking.concert_booking.user.entity.User lockedByUser;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Version
    private Long version;
}
