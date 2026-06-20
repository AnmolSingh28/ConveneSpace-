package com.concertbooking.concert_booking.booking.repository;

import com.concertbooking.concert_booking.booking.entity.Booking;
import com.concertbooking.concert_booking.common.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    @Query(
            value = """
        SELECT DISTINCT b
        FROM Booking b
        LEFT JOIN FETCH b.concert c
        LEFT JOIN FETCH c.venue
        LEFT JOIN FETCH b.items bi
        LEFT JOIN FETCH bi.tier t
        LEFT JOIN FETCH t.section
        LEFT JOIN FETCH bi.seatInventory
        WHERE b.user.id = :userId
        """,
            countQuery = """
        SELECT COUNT(DISTINCT b)
        FROM Booking b
        WHERE b.user.id = :userId
        """
    )
    Page<Booking> findByUserId(
            @Param("userId") UUID userId,
            Pageable pageable
    );

    @Query("""

SELECT DISTINCT b
FROM Booking b
LEFT JOIN FETCH b.concert c
LEFT JOIN FETCH c.venue
LEFT JOIN FETCH b.items bi
LEFT JOIN FETCH bi.tier t
LEFT JOIN FETCH t.section
LEFT JOIN FETCH bi.seatInventory
WHERE b.bookingReference = :bookingReference
""")
    Optional<Booking> findByBookingReference(
            @Param("bookingReference") String bookingReference
    );

    Optional<Booking> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    Page<Booking> findByUserIdAndStatus(
            UUID userId, BookingStatus status, Pageable pageable);


    @Query("SELECT b FROM Booking b WHERE b.concert.id = :concertId " +
            "AND b.status = :status")
    Page<Booking> findByConcertIdAndStatus(
            @Param("concertId") UUID concertId,
            @Param("status") BookingStatus status,
            Pageable pageable);


    @Query("SELECT SUM(b.totalAmount) FROM Booking b " +
            "WHERE b.concert.id = :concertId AND b.status = 'CONFIRMED'")
    java.math.BigDecimal getTotalRevenueByConcert(
            @Param("concertId") UUID concertId);


    @Query("""
    SELECT b FROM Booking b
    LEFT JOIN FETCH b.items bi
    LEFT JOIN FETCH bi.tier t
    LEFT JOIN FETCH bi.seatInventory
    WHERE b.status = 'PENDING'
    AND b.createdAt < :cutoff
    """)
    List<Booking> findExpiredPendingBookings(@Param("cutoff") LocalDateTime cutoff);

  
    @Query("""

SELECT COALESCE(SUM(b.totalAmount),0)
FROM Booking b
WHERE b.concert.organizer.id=:organizerId
AND b.status='CONFIRMED'
""")
    BigDecimal getTotalRevenue(UUID organizerId);
    
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status = 'PENDING' " + "AND b.createdAt < :cutoff")
    long countExpiredPendingBookings(@Param("cutoff") LocalDateTime cutoff);
    boolean existsByUserIdAndConcertIdAndStatusIn(UUID userId, UUID concertId, List<BookingStatus> statuses);

    @Query("""
SELECT COALESCE(SUM(b.totalAmount), 0)
FROM Booking b
WHERE b.status = com.concertbooking.concert_booking.common.enums.BookingStatus.CONFIRMED
""")
    BigDecimal totalRevenue();
}


