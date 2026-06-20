package com.concertbooking.concert_booking.concert.repository;

import com.concertbooking.concert_booking.common.enums.TierStatus;
import com.concertbooking.concert_booking.concert.entity.TicketTier;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketTierRepository extends JpaRepository<TicketTier, UUID> {
    @Query("""
SELECT t
FROM TicketTier t
LEFT JOIN FETCH t.section
WHERE t.concert.id = :concertId
""")
    List<TicketTier>findByConcertIdWithSection(@Param("concertId") UUID concertId);

    @Query("""
SELECT t FROM TicketTier t
LEFT JOIN FETCH t.section
WHERE t.concert.id = :concertId AND t.isActive = true
""")
    List<TicketTier>findByConcertIdAndIsActiveTrue(UUID concertId);

    List<TicketTier>findByConcertIdAndTierStatus(UUID concertId, TierStatus status);


    @Modifying
    @Query("UPDATE TicketTier t SET t.availableQuantity = t.availableQuantity - :quantity " +
            "WHERE t.id = :tierId AND t.availableQuantity >= :quantity")
    int decrementAvailableQuantity(
            @Param("tierId") UUID tierId,
            @Param("quantity") int quantity);


    @Modifying
    @Query("UPDATE TicketTier t SET t.availableQuantity = t.availableQuantity + :quantity " +
            "WHERE t.id = :tierId")
    int incrementAvailableQuantity(
            @Param("tierId") UUID tierId,
            @Param("quantity") int quantity);

}
