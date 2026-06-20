package com.concertbooking.concert_booking.review.repository;

import com.concertbooking.concert_booking.review.entity.OrganizerReview;
import com.concertbooking.concert_booking.review.mapper.RatingDistributionProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;



public interface OrganizerReviewRepository  extends JpaRepository<OrganizerReview, UUID> {

    boolean existsByReviewerIdAndConcertId(UUID reviewerId, UUID concertId);

    @Query("""
        SELECT COALESCE(AVG(r.rating),0)
        FROM OrganizerReview r
        WHERE r.organizer.id = :organizerId
    """)
    Double getAverageRating(@Param("organizerId") UUID organizerId);

    long countByOrganizerId(UUID organizerId);

    @Query("""
    SELECT
        r.rating,
        COUNT(r)
    FROM OrganizerReview r
    WHERE r.organizer.id = :organizerId
    GROUP BY r.rating
    ORDER BY r.rating DESC
""")
    List<Object[]> getRatingDistribution(@Param("organizerId") UUID organizerId
    );
    Page<OrganizerReview> findByOrganizerId(UUID organizerId, Pageable pageable);



}
