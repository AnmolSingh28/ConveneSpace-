package com.concertbooking.concert_booking.review.service;

import com.concertbooking.concert_booking.booking.entity.Booking;
import com.concertbooking.concert_booking.booking.entity.BookingItem;
import com.concertbooking.concert_booking.booking.repository.BookingRepository;
import com.concertbooking.concert_booking.common.enums.BookingStatus;
import com.concertbooking.concert_booking.common.exception.BookingException;
import com.concertbooking.concert_booking.common.exception.ResourceNotFoundException;
import com.concertbooking.concert_booking.notification.service.NotificationPublisher;
import com.concertbooking.concert_booking.review.dto.CreateOrganizerReviewRequest;
import com.concertbooking.concert_booking.review.dto.OrganizerRatingSummaryResponse;
import com.concertbooking.concert_booking.review.dto.OrganizerReviewResponse;
import com.concertbooking.concert_booking.review.dto.RatingDistributionResponse;
import com.concertbooking.concert_booking.review.entity.OrganizerReview;
import com.concertbooking.concert_booking.review.event.ReviewCreatedEvent;
import com.concertbooking.concert_booking.review.mapper.OrganizerReviewMapper;
import com.concertbooking.concert_booking.review.repository.OrganizerReviewRepository;
import com.concertbooking.concert_booking.user.entity.User;
import com.concertbooking.concert_booking.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor

@Slf4j
public class OrganizerReviewService {
    private final OrganizerReviewRepository organizerReviewRepository;
    private final BookingRepository bookingRepository;
    private final OrganizerReviewMapper organizerReviewMapper;
    private final UserRepository userRepository;
    private final NotificationPublisher notificationPublisher;
    @Transactional
    public void createReview(UUID bookingId,CreateOrganizerReviewRequest request,User currentUser) {
        Booking booking=getEligibleBooking(bookingId,currentUser.getId());

        preventDuplicateReview(currentUser.getId(),booking.getConcert().getId());

        OrganizerReview review=OrganizerReview.builder()
                .organizer(booking.getConcert().getOrganizer())
                .reviewer(currentUser)
                .concert(booking.getConcert())
                .rating(request.rating())
                .reviewText(request.reviewText())
                .wouldAttendAgain(request.wouldAttendAgain())
                .build();

        organizerReviewRepository.save(review);
        ReviewCreatedEvent event=ReviewCreatedEvent.builder().organizerId(
                                review.getOrganizer().getId()
                        ).rating(review.getRating())
                        .build();

        notificationPublisher.publishReviewCreated(event);
        log.info(
                "Organizer review submitted. bookingId={}, concertId={}, organizerId={}, reviewerId={}, rating={}",
                booking.getId(),
                booking.getConcert().getId(),
                booking.getConcert().getOrganizer().getId(),
                currentUser.getId(),
                request.rating()
        );
    }

    private Booking getEligibleBooking(UUID bookingId, UUID userId
    ) {
        Booking booking=bookingRepository.findById(bookingId).orElseThrow(()->new ResourceNotFoundException(
                                "Booking not found"));

        validateBookingOwnership(booking,userId);
        validateConcertEnded(booking);
        validateAttendance(booking);
        validateBookingStatus(booking);

        return booking;
    }

    private void validateBookingOwnership(Booking booking, UUID userId
    ) {
        if (!booking.getUser().getId().equals(userId)) {
            throw new BookingException("You can only review your own booking");
        }
    }

    private void validateConcertEnded(Booking booking){
        if (booking.getConcert().getConcertDate().isAfter(LocalDateTime.now())) {
            throw new BookingException("Reviews are allowed only after event completion");
        }
    }

    private void validateAttendance(Booking booking){
        boolean attended=booking.getItems().stream().anyMatch(BookingItem::isCheckedIn);
        if (!attended) {
            throw new BookingException("Only attendees can review organizers");
        }
    }

    private void preventDuplicateReview(UUID reviewerId,UUID concertId) {

        if (organizerReviewRepository.existsByReviewerIdAndConcertId(reviewerId,concertId)) {
            log.warn("Duplicate review attempt. reviewerId={}, concertId={}",reviewerId,concertId);
            throw new BookingException("You have already reviewed this event");

        }

    }
    private void validateBookingStatus(Booking booking) {
        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.ATTENDED) {
            throw new BookingException("Only confirmed/attended bookings can review organizers");
        }
    }
    public Page<OrganizerReviewResponse> getOrganizerReviews(UUID organizerId, Pageable pageable
    ) {
        return organizerReviewRepository.findByOrganizerId(organizerId, pageable)
                .map(organizerReviewMapper::toResponse);
    }
    public OrganizerRatingSummaryResponse getOrganizerRatingSummary(UUID organizerId) {
        String organizerName = userRepository.findById(organizerId)
                .map(u -> u.getName())
                .orElse("Organizer");

        Double averageRating = organizerReviewRepository.getAverageRating(organizerId);
        Long totalReviews = organizerReviewRepository.countByOrganizerId(organizerId);
        List<RatingDistributionResponse> distribution = organizerReviewRepository
                .getRatingDistribution(organizerId)
                .stream()
                .map(row -> new RatingDistributionResponse(
                        ((Number) row[0]).intValue(),
                        ((Number) row[1]).longValue()
                ))
                .toList();

        return new OrganizerRatingSummaryResponse(
                organizerName,
                averageRating,
                totalReviews,
                distribution
        );
    }
}
