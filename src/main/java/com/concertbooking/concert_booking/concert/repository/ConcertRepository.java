package com.concertbooking.concert_booking.concert.repository;

import com.concertbooking.concert_booking.common.enums.ConcertStatus;
import com.concertbooking.concert_booking.common.enums.EventType;
import com.concertbooking.concert_booking.concert.dto.ConcertSummaryResponse;
import com.concertbooking.concert_booking.concert.entity.Concert;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConcertRepository extends JpaRepository<Concert, UUID> {

    @Query("""
    SELECT new com.concertbooking.concert_booking.concert.dto.ConcertSummaryResponse(
        c.id, c.title, c.artistName, c.bannerImageUrl,
        v.name, v.city, c.concertDate, c.saleStartTime,
        c.status, c.isFeatured, c.requiresPreRegistration, MIN(t.price),c.category.name,0.0,
            c.organizer.id, c.organizer.name
    )
    FROM Concert c
    JOIN c.venue v
    JOIN c.ticketTiers t
    WHERE c.concertDate > :now
    AND c.status = 'PUBLISHED'
    AND t.isActive = true
    GROUP BY c.id, c.title, c.artistName, c.bannerImageUrl,
        v.name, v.city, c.concertDate, c.saleStartTime,
        c.status, c.isFeatured, c.requiresPreRegistration,
           c.category.name, c.organizer.id, c.organizer.name
    ORDER BY c.concertDate ASC
    """)
    Page<ConcertSummaryResponse> findUpcomingConcerts(
            @Param("now") LocalDateTime now,
            Pageable pageable);

    @Query("""
    SELECT new com.concertbooking.concert_booking.concert.dto.ConcertSummaryResponse(
        c.id, c.title, c.artistName, c.bannerImageUrl,
        v.name, v.city, c.concertDate, c.saleStartTime,
        c.status, c.isFeatured, c.requiresPreRegistration, MIN(t.price),c.category.name,0.0,
            c.organizer.id, c.organizer.name
    )
    FROM Concert c
    JOIN c.venue v
    JOIN c.ticketTiers t
    WHERE v.city = :city
    AND c.status = :status
    AND t.isActive = true
    GROUP BY c.id, c.title, c.artistName, c.bannerImageUrl,
        v.name, v.city, c.concertDate, c.saleStartTime,
        c.status, c.isFeatured, c.requiresPreRegistration,c.category.name,
            c.organizer.id, c.organizer.name
    ORDER BY c.concertDate ASC
    """)
    Page<ConcertSummaryResponse> findByCityProjection(
            @Param("city") String city,
            @Param("status") ConcertStatus status,
            Pageable pageable);

    @Query("""
    SELECT new com.concertbooking.concert_booking.concert.dto.ConcertSummaryResponse(
        c.id, c.title, c.artistName, c.bannerImageUrl,
        v.name, v.city, c.concertDate, c.saleStartTime,
        c.status, c.isFeatured, c.requiresPreRegistration, MIN(t.price),c.category.name,0.0,
            c.organizer.id, c.organizer.name
    )
    FROM Concert c
    JOIN c.venue v
    JOIN c.ticketTiers t
    WHERE (LOWER(c.title) LIKE LOWER(CONCAT('%', :query, '%'))
        OR LOWER(c.artistName) LIKE LOWER(CONCAT('%', :query, '%')))
    AND c.status = :status
    AND t.isActive = true
    GROUP BY c.id, c.title, c.artistName, c.bannerImageUrl,
        v.name, v.city, c.concertDate, c.saleStartTime,
        c.status, c.isFeatured, c.requiresPreRegistration,c.category.name,c.organizer.id, c.organizer.name
    ORDER BY c.concertDate ASC
    """)
    Page<ConcertSummaryResponse> findBySearchProjection(
            @Param("query") String query,
            @Param("status") ConcertStatus status,
            Pageable pageable);

    @Query("""
    SELECT new com.concertbooking.concert_booking.concert.dto.ConcertSummaryResponse(
        c.id, c.title, c.artistName, c.bannerImageUrl,
        v.name, v.city, c.concertDate, c.saleStartTime,
        c.status, c.isFeatured, c.requiresPreRegistration, MIN(t.price),c.category.name,0.0,
            c.organizer.id, c.organizer.name
    )
    FROM Concert c
    JOIN c.venue v
    JOIN c.ticketTiers t
    WHERE c.isFeatured = true
    AND c.status = :status
    AND c.concertDate > :now
    AND t.isActive = true
    GROUP BY c.id, c.title, c.artistName, c.bannerImageUrl,
        v.name, v.city, c.concertDate, c.saleStartTime,
        c.status, c.isFeatured, c.requiresPreRegistration,c.category.name,c.organizer.id, c.organizer.name
    """)
    List<ConcertSummaryResponse> findFeaturedProjection(
            @Param("status") ConcertStatus status,
            @Param("now") LocalDateTime now);

    @Query("""
    SELECT new com.concertbooking.concert_booking.concert.dto.ConcertSummaryResponse(
        c.id, c.title, c.artistName, c.bannerImageUrl,
        v.name, v.city, c.concertDate, c.saleStartTime,
        c.status, c.isFeatured, c.requiresPreRegistration, MIN(t.price),c.category.name,0.0,
            c.organizer.id, c.organizer.name
    )
    FROM Concert c
    JOIN c.venue v
    LEFT JOIN c.ticketTiers t
    WHERE c.organizer.id = :organizerId
    AND (t IS NULL OR t.isActive = true)
    GROUP BY c.id, c.title, c.artistName, c.bannerImageUrl,
        v.name, v.city, c.concertDate, c.saleStartTime,
        c.status, c.isFeatured, c.requiresPreRegistration,c.category.name,c.organizer.id, c.organizer.name
    ORDER BY c.concertDate DESC
    """)
    Page<ConcertSummaryResponse> findOrganizerConcertsProjection(
            @Param("organizerId") UUID organizerId,
            Pageable pageable);

    @Query("""
    SELECT new com.concertbooking.concert_booking.concert.dto.ConcertSummaryResponse(
        c.id, c.title, c.artistName, c.bannerImageUrl,
        v.name, v.city, c.concertDate, c.saleStartTime,
        c.status, c.isFeatured, c.requiresPreRegistration, MIN(t.price),
        c.category.name, null, c.organizer.id, c.organizer.name
    )
    FROM Concert c
    JOIN c.venue v
    JOIN c.ticketTiers t
    WHERE c.category.id = :categoryId
    AND c.status = 'PUBLISHED'
    AND c.concertDate > :now
    AND t.isActive = true
    AND c.status = 'PUBLISHED'
    AND c.concertDate > :now
    AND t.isActive = true
    GROUP BY c.id, c.title, c.artistName, c.bannerImageUrl,
        v.name, v.city, c.concertDate, c.saleStartTime,
        c.status, c.isFeatured, c.requiresPreRegistration, c.category.name,c.organizer.id, c.organizer.name
    ORDER BY c.concertDate ASC
    """)
    Page<ConcertSummaryResponse> findByCategoryProjection(
            @Param("categoryId") UUID categoryId,
            @Param("now") LocalDateTime now,
            Pageable pageable);
    //Haversine query
    @Query(value = """
SELECT *
FROM (
    SELECT
        c.id::text AS concert_id,
        (
            6371 * acos(
                cos(radians(:lat)) * cos(radians(v.latitude))
                * cos(radians(v.longitude) - radians(:lng))
                + sin(radians(:lat)) * sin(radians(v.latitude))
            )
        ) AS distance_km
    FROM concerts c
    JOIN venues v ON c.venue_id = v.id
    WHERE c.status = 'PUBLISHED'
      AND c.concert_date > NOW()
      AND v.latitude IS NOT NULL
      AND v.longitude IS NOT NULL
) nearby
WHERE nearby.distance_km <= :radiusKm
ORDER BY nearby.distance_km ASC
LIMIT :limitVal OFFSET :offsetVal
""", nativeQuery = true)
    List<Object[]> findNearbyRaw(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusKm") double radiusKm,
            @Param("limitVal") int limitVal,
            @Param("offsetVal") int offsetVal);
    @Query(value = """
    SELECT COUNT(*)
    FROM concerts c
    JOIN venues v ON c.venue_id = v.id
    WHERE c.status = 'PUBLISHED'
    AND c.concert_date > NOW()
    AND v.latitude IS NOT NULL
    AND v.longitude IS NOT NULL
    AND (6371 * acos(
        cos(radians(:lat)) * cos(radians(v.latitude))
        * cos(radians(v.longitude) - radians(:lng))
        + sin(radians(:lat)) * sin(radians(v.latitude))
    )) <= :radiusKm
    """, nativeQuery = true)
    long countNearby(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusKm") double radiusKm);

    @Query("""
SELECT DISTINCT c
FROM Concert c
LEFT JOIN FETCH c.venue v
LEFT JOIN FETCH v.sections
LEFT JOIN FETCH c.organizer
WHERE c.id = :id
""")
    Optional<Concert> findByIdWithVenue(@Param("id") UUID id);

    long countByOrganizerId(UUID organizerId);
    List<Concert> findByRequiresPreRegistrationTrueAndStatus(ConcertStatus status);

    @Query("""
SELECT new com.concertbooking.concert_booking.concert.dto.ConcertSummaryResponse(
    c.id, c.title, c.artistName, c.bannerImageUrl,
    v.name, v.city, c.concertDate, c.saleStartTime,
    c.status, c.isFeatured, c.requiresPreRegistration, MIN(t.price), c.category.name, 0.0,
    c.organizer.id, c.organizer.name
)
FROM Concert c
JOIN c.venue v
LEFT JOIN c.ticketTiers t
GROUP BY c.id, c.title, c.artistName, c.bannerImageUrl,
    v.name, v.city, c.concertDate, c.saleStartTime,
    c.status, c.isFeatured, c.requiresPreRegistration, c.category.name,
    c.organizer.id, c.organizer.name
ORDER BY c.concertDate DESC
""")
    Page<ConcertSummaryResponse> findAllConcertsProjection(Pageable pageable);
}
