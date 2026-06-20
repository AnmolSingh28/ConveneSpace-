package com.concertbooking.concert_booking.seat.repository;

import com.concertbooking.concert_booking.common.enums.SeatStatus;
import com.concertbooking.concert_booking.seat.entity.SeatInventory;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SeatInventoryRepository extends JpaRepository<SeatInventory, UUID> {


    List<SeatInventory> findByTierId(UUID tierId);

    List<SeatInventory> findByTierIdAndStatus(UUID tierId, SeatStatus status);

    Optional<SeatInventory> findByTierIdAndRowLabelAndSeatNumber(
            UUID tierId, String rowLabel, String seatNumber);

    // Expired locks cleanup
    @Query("SELECT s FROM SeatInventory s WHERE " +
            "s.status = 'LOCKED' AND s.lockedUntil < :now")
    List<SeatInventory> findExpiredLocks(@Param("now") LocalDateTime now);

    // Release expired locks atomically
    @Modifying
    @Query("UPDATE SeatInventory s SET s.status = 'AVAILABLE', " +
            "s.lockedByUser = null, s.lockedUntil = null " +
            "WHERE s.status = 'LOCKED' AND s.lockedUntil < :now")
    int releaseExpiredLocks(@Param("now") LocalDateTime now);

    // Count available seats for a tier
    long countByTierIdAndStatus(UUID tierId, SeatStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name="jakarta.persistence.lock.timeout",value = "-2"))
    @Query("SELECT s FROM SeatInventory s WHERE s.tier.id =:tierId " +
            "AND s.status ='AVAILABLE' ORDER BY s.rowLabel ASC,s.seatNumber ASC")
    List<SeatInventory>findAvailableSeatsForLocking(@Param("tierId")UUID tierId);

    @Query("SELECT s FROM SeatInventory s WHERE s.lockedByUser.id = :userId AND s.status = :status")
    List<SeatInventory> findByLockedByUserIdAndStatus(
            @Param("userId") UUID userId,
            @Param("status") SeatStatus status
    );
    List<SeatInventory> findByStatus(SeatStatus status);

    @Query("SELECT COUNT(s) FROM SeatInventory s WHERE s.lockedByUser.id = :userId " +
            "AND s.tier.concert.id = :concertId AND s.status = 'LOCKED'")
    int countByLockedByUserIdAndConcertId(
            @Param("userId") UUID userId,
            @Param("concertId") UUID concertId);

    Optional<SeatInventory> findFirstByLockedByUserIdAndStatus(
            UUID userId,
            SeatStatus status
    );
    long countByStatus(SeatStatus status);
    List<SeatInventory> findByLockedByUserId(UUID userId);
}
