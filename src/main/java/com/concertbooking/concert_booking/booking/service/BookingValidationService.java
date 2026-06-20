package com.concertbooking.concert_booking.booking.service;

import com.concertbooking.concert_booking.booking.dto.BookingRequest;
import com.concertbooking.concert_booking.booking.mapper.BookingMapper;
import com.concertbooking.concert_booking.booking.repository.BookingItemRepository;
import com.concertbooking.concert_booking.booking.repository.BookingRepository;
import com.concertbooking.concert_booking.common.enums.ConcertStatus;
import com.concertbooking.concert_booking.common.enums.SectionType;
import com.concertbooking.concert_booking.common.enums.TierStatus;
import com.concertbooking.concert_booking.common.exception.BookingException;
import com.concertbooking.concert_booking.concert.entity.Concert;
import com.concertbooking.concert_booking.concert.entity.TicketTier;
import com.concertbooking.concert_booking.concert.repository.PreRegistrationRepository;
import com.concertbooking.concert_booking.seat.repository.SeatInventoryRepository;
import com.concertbooking.concert_booking.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingValidationService {
    private final BookingMapper bookingMapper;
    private final BookingRepository bookingRepository;
    private final PreRegistrationRepository preRegistrationRepository;
    private final BookingItemRepository bookingItemRepository;
    private final SeatInventoryRepository seatInventoryRepository;
    public void validateBookingRequest(
            BookingRequest request,TicketTier tier,User user){

        validateConcert(tier.getConcert());
        validateTier(tier);
        validatePreRegistration(tier.getConcert(),user);
        validateQuantity(request,tier);
        validateNoDuplicateBooking(tier,user);
    }
    private void validateConcert(Concert concert) {
        // if the concert is not having a PUBLISHED state
        if (concert.getStatus()!=ConcertStatus.PUBLISHED) {
            throw new BookingException("Concert is not available for booking");
        }
        LocalDateTime now = LocalDateTime.now();
        //if the user has came before the ticket sale
        if (now.isBefore(concert.getSaleStartTime())) {
            throw new BookingException("Ticket sale has not yet started");
        }
        // now is after means if time has already passed the sale time
        if (concert.getSaleEndTime() != null && now.isAfter(concert.getSaleEndTime())) {
            throw new BookingException("Ticket sale has now ended");
        }
    }
    private void validateTier(TicketTier tier) {
        if (tier.getTierStatus()!=TierStatus.ACTIVE) {
            throw new BookingException("This ticket tier is not currently active");
        }
    }
    private void validatePreRegistration(Concert concert,User user) {
        if (!concert.isRequiresPreRegistration()) return;
        boolean registered=preRegistrationRepository
                .existsByUserIdAndConcertId(user.getId(),concert.getId());
        if (!registered) {
            throw new BookingException("This concert requires pre-registration." +
                    " You are not yet registered.");
        }
    }
    private void validateQuantity(BookingRequest request, TicketTier tier) {
        boolean isGa = tier.getSection().getSectionType() == SectionType.GA;

        int requested = isGa
                ?(request.getQuantity()!=null ? request.getQuantity(): 0)
                :(request.getSeatIds()!=null ? request.getSeatIds().size(): 0);

        if (requested<1) {
            throw new BookingException("At least 1 ticket must be selected");
        }

        if (requested > tier.getMaxPerUser()) {
            throw new BookingException(
                    "Maximum " +tier.getMaxPerUser() +
                            " tickets allowed per user");
        }
    }
    private void validateNoDuplicateBooking(TicketTier tier, User user) {
        int confirmedCount = bookingItemRepository
                .countConfirmedSeatsForUserAndConcert(
                        user.getId(),
                        tier.getConcert().getId());

        if (confirmedCount >= tier.getMaxPerUser()) {
            throw new BookingException(
                    "Maximum " + tier.getMaxPerUser() +
                            " tickets allowed per user for this concert");
        }
    }

}
