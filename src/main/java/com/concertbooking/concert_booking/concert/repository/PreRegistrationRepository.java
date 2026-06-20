package com.concertbooking.concert_booking.concert.repository;

import com.concertbooking.concert_booking.concert.entity.ConcertPreRegistration;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface PreRegistrationRepository extends JpaRepository<ConcertPreRegistration, UUID> {

    boolean existsByUserIdAndConcertId(UUID userId,UUID concertId);

    @Query("""
SELECT r
FROM ConcertPreRegistration r
LEFT JOIN FETCH r.concert c
WHERE r.user.id = :userId
AND c.id = :concertId
""")
    Optional<ConcertPreRegistration> findByUserIdAndConcertId(
            @Param("userId") UUID userId,
            @Param("concertId") UUID concertId
    );

    long countByConcertId(UUID concertId);

    @Query("SELECT r FROM ConcertPreRegistration r " +
            "WHERE r.concert.id= :concertId " +
           "ORDER BY r.queuePosition ASC")
    List<ConcertPreRegistration>findByConcertIdOrderByPosition(
            @Param("concertId") UUID concertId);


}
