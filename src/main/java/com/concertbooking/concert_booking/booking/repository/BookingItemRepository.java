package com.concertbooking.concert_booking.booking.repository;

import com.concertbooking.concert_booking.booking.entity.BookingItem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingItemRepository extends JpaRepository<BookingItem,UUID> {

    @Query("""
SELECT bi
FROM BookingItem bi
LEFT JOIN FETCH bi.tier t
LEFT JOIN FETCH t.section
LEFT JOIN FETCH bi.seatInventory
WHERE bi.booking.id IN :bookingIds
""")
    List<BookingItem> findByBookingIdIn(
            @Param("bookingIds") List<UUID> bookingIds
    );

    @Query("""
SELECT bi FROM BookingItem bi
LEFT JOIN FETCH bi.booking b
LEFT JOIN FETCH b.user
LEFT JOIN FETCH bi.tier t
WHERE bi.qrToken = :qrToken
""")
    Optional<BookingItem> findByQrToken(@Param("qrToken") String qrToken);


    @Query("SELECT bi FROM BookingItem bi WHERE " +
           "bi.booking.concert.id= :concertId AND bi.checkedIn=false ")
    List<BookingItem> findUncheckedInByConcert(
            @Param( "concertId") UUID concertId);

    @Query("SELECT bi FROM BookingItem bi " +
            "WHERE bi.booking.status = com.concertbooking.concert_booking.common.enums.BookingStatus.CONFIRMED " +
            "AND bi.checkedIn = false")
    List<BookingItem> findConfirmedBookingItems();

    @Query("SELECT COUNT(bi) FROM BookingItem bi WHERE bi.booking.user.id = :userId " +
            "AND bi.booking.concert.id = :concertId " +
            "AND bi.booking.status = 'CONFIRMED'")
    int countConfirmedSeatsForUserAndConcert(
            @Param("userId") UUID userId,
            @Param("concertId") UUID concertId);

    @Query("""
SELECT COALESCE(SUM(bi.quantity),0)
FROM BookingItem bi
WHERE bi.booking.concert.organizer.id=:organizerId
AND bi.booking.status='CONFIRMED'
""")
    Long getTotalTicketsSold(UUID organizerId);

    @Query("""
SELECT COUNT(bi)
FROM BookingItem bi
WHERE bi.booking.concert.organizer.id=:organizerId
AND bi.checkedIn=true
""")
    Long getCheckedInCount(UUID organizerId);

}
