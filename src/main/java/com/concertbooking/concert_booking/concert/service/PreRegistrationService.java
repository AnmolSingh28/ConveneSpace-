package com.concertbooking.concert_booking.concert.service;

import com.concertbooking.concert_booking.common.exception.BookingException;
import com.concertbooking.concert_booking.common.exception.ResourceNotFoundException;
import com.concertbooking.concert_booking.concert.dto.PreRegistrationResponse;
import com.concertbooking.concert_booking.concert.entity.Concert;
import com.concertbooking.concert_booking.concert.entity.ConcertPreRegistration;
import com.concertbooking.concert_booking.concert.mapper.PreRegistrationMapper;
import com.concertbooking.concert_booking.concert.repository.ConcertRepository;
import com.concertbooking.concert_booking.concert.repository.PreRegistrationRepository;
import com.concertbooking.concert_booking.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PreRegistrationService {
    private  final PreRegistrationRepository preRegistrationRepository;
    private final ConcertRepository concertRepository;
    private final PreRegistrationMapper preRegistrationMapper;

    @Transactional
    public PreRegistrationResponse register(UUID concertId, User user){
        Concert concert=concertRepository.findById(concertId)
                .orElseThrow(()->new ResourceNotFoundException("Concert not found:"+concertId));
        if(!concert.isRequiresPreRegistration()){
            throw new BookingException("This concert does not require pre-registration");
        }
        LocalDateTime now=LocalDateTime.now();

        if(concert.getPreRegistrationStart()!=null && now.isBefore(concert.getPreRegistrationStart())){
            throw new BookingException(
                    "Pre registration has not started yet"
            );
        }
        if (preRegistrationRepository.existsByUserIdAndConcertId(
                user.getId(), concertId)) {
            throw new BookingException(
                    "You are already registered for this concert");
        }

        ConcertPreRegistration registration=ConcertPreRegistration.builder()
                .user(user)
                .concert(concert)
                .hasPurchased(false)
                .build();

        ConcertPreRegistration saved=preRegistrationRepository.save(registration);

        log.info("User {} registered for concert {}",
                user.getEmail(), concert.getTitle());

        return preRegistrationMapper.toResponse(saved);
    }
    @Transactional
    public void assignQueuePositions(UUID concertId, User organizer) {

        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Concert not found: " + concertId));

        if (!concert.getOrganizer().getId().equals(organizer.getId())) {
            throw new BookingException(
                    "You are not the organizer of this concert");
        }

        List<ConcertPreRegistration> registrations =
                preRegistrationRepository.findByConcertIdOrderByPosition(concertId);

        if (registrations.isEmpty()) {
            throw new BookingException(
                    "No registrations found for this concert");
        }

        //Shuffle randomized fair queue

        SecureRandom random = new SecureRandom();
        for (int i=registrations.size()-1;i>0;i--){
            int j = random.nextInt(i + 1);
            ConcertPreRegistration temp = registrations.get(i);
            registrations.set(i, registrations.get(j));
            registrations.set(j, temp);
        }

        // Assign positions and purchase windows
        LocalDateTime saleStart = concert.getSaleStartTime();
        int windowMinutes = 30;

        for (int i=0;i<registrations.size();i++){
            ConcertPreRegistration reg = registrations.get(i);
            reg.setQueuePosition(i + 1);
            reg.setPurchaseWindowStart(
                    saleStart.plusMinutes((long) i * windowMinutes));
            reg.setPurchaseWindowEnd(
                    saleStart.plusMinutes((long) (i + 1) * windowMinutes));
        }

        preRegistrationRepository.saveAll(registrations);

        log.info("Queue positions assigned for concert {} — {} registrations",
                concert.getTitle(), registrations.size());
    }


    public PreRegistrationResponse getMyRegistration(UUID concertId, User user){
        ConcertPreRegistration registration = preRegistrationRepository
                .findByUserIdAndConcertId(user.getId(), concertId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "You are not registered for this concert"));
        return preRegistrationMapper.toResponse(registration);
    }


    public long getRegistrationCount(UUID concertId) {
        concertRepository.findById(concertId)
                .orElseThrow(() -> new ResourceNotFoundException("Concert not found: " + concertId));

        return preRegistrationRepository.countByConcertId(concertId);
    }

}
