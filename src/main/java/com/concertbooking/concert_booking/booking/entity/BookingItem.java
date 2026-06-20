package com.concertbooking.concert_booking.booking.entity;

import com.concertbooking.concert_booking.concert.entity.TicketTier;
import com.concertbooking.concert_booking.seat.entity.SeatInventory;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "booking_items", indexes = {
        @Index(name = "idx_item_booking", columnList = "booking_id"),
        @Index(name = "idx_item_tier", columnList = "tier_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"booking", "tier", "seatInventory"})
public class BookingItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tier_id", nullable = false)
    private TicketTier tier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_inventory_id")
    private SeatInventory seatInventory;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "price_at_booking", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAtBooking;

    @Column(name = "qr_code_url",columnDefinition = "TEXT")
    private String qrCodeUrl;

    @Column(name = "qr_token")
    private String qrToken;

    @Column(name = "qr_token_expires_at")
    private java.time.LocalDateTime qrTokenExpiresAt;

    @Column(name = "is_checked_in", nullable = false)
    private boolean checkedIn = false;

    @Column(name = "checked_in_at")
    private java.time.LocalDateTime checkedInAt;
}
