package com.concertbooking.concert_booking.venue.repository;

import com.concertbooking.concert_booking.venue.entity.Venue.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
@Repository
public interface VenueRepository extends JpaRepository<Venue, UUID> {
    List<Venue> findByCityIgnoreCase(String city);
    List<Venue> findByVenueType(Venue venueType);
    List<Venue> findByCityIgnoreCaseAndIsActiveTrue(String city);
    List<Venue> findByNameContainingIgnoreCaseAndIsActiveTrue(String name);
    boolean existsByNameIgnoreCase(String name);
    @Query("SELECT v FROM Venue v WHERE v.isActive=true ORDER BY v.createdAt DESC")
    List<Venue> findAllActive();
    @Query("""
SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END
FROM Venue v
WHERE LOWER(v.name) = LOWER(:name)
AND v.isActive = true
""")
    boolean existsActiveVenueByName(@Param("name") String name);
    boolean existsByNameIgnoreCaseAndIsActiveTrue(String name);
}
