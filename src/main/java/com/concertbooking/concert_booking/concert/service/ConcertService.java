package com.concertbooking.concert_booking.concert.service;

import com.concertbooking.concert_booking.common.enums.*;
import com.concertbooking.concert_booking.common.exception.BookingException;
import com.concertbooking.concert_booking.common.exception.ResourceNotFoundException;
import com.concertbooking.concert_booking.common.exception.AuthException;
import com.concertbooking.concert_booking.common.response.PageResponseBuilder;
import com.concertbooking.concert_booking.common.response.PagedResponse;
import com.concertbooking.concert_booking.concert.dto.*;
import com.concertbooking.concert_booking.concert.entity.Concert;
import com.concertbooking.concert_booking.concert.entity.EventCategory;
import com.concertbooking.concert_booking.concert.entity.TicketTier;
import com.concertbooking.concert_booking.concert.mapper.ConcertMapper;
import com.concertbooking.concert_booking.concert.repository.ConcertRepository;
import com.concertbooking.concert_booking.concert.repository.EventCategoryRepository;
import com.concertbooking.concert_booking.concert.repository.TicketTierRepository;
import com.concertbooking.concert_booking.review.repository.OrganizerReviewRepository;
import com.concertbooking.concert_booking.seat.entity.SeatInventory;
import com.concertbooking.concert_booking.seat.repository.SeatInventoryRepository;
import com.concertbooking.concert_booking.user.entity.User;

import com.concertbooking.concert_booking.venue.entity.Venue.Venue;
import com.concertbooking.concert_booking.venue.entity.Venue.VenueSection;
import com.concertbooking.concert_booking.venue.mapper.VenueMapper;
import com.concertbooking.concert_booking.venue.repository.VenueRepository;
import com.concertbooking.concert_booking.venue.repository.VenueSectionRepository;
import com.concertbooking.concert_booking.venue.dto.VenueResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
@Slf4j
public class ConcertService {
    private final ConcertRepository concertRepository;
    private final TicketTierRepository ticketTierRepository;
    private final VenueRepository venueRepository;
    private final VenueSectionRepository venueSectionRepository;
    private final ConcertMapper concertMapper;
    private final VenueMapper venueMapper;
    private final SeatInventoryRepository seatInventoryRepository;
    private final OrganizerReviewRepository organizerReviewRepository;
    private final EventCategoryRepository eventCategoryRepository;
    //HELPERS:
    private ConcertResponse buildConcertResponse(Concert concert) {
        List<TicketTierResponse> tiers = ticketTierRepository
                .findByConcertIdAndIsActiveTrue(concert.getId())
                .stream()
                .map(concertMapper::toTierResponse)
                .collect(Collectors.toList());

        VenueResponse venue=venueMapper.toResponse(concert.getVenue());
        Double organizerRating = null;
        UUID organizerId = null;
        String organizerName = null;
        if (concert.getOrganizer() != null) {
            organizerId = concert.getOrganizer().getId();
            organizerName = concert.getOrganizer().getName();
            organizerRating = organizerReviewRepository.getAverageRating(organizerId);
        }
        return ConcertResponse.builder()
                .id(concert.getId())
                .title(concert.getTitle())
                .description(concert.getDescription())
                .artistName(concert.getArtistName())
                .bannerImageUrl(concert.getBannerImageUrl())
                .concertDate(concert.getConcertDate())
                .saleStartTime(concert.getSaleStartTime())
                .saleEndTime(concert.getSaleEndTime())
                .status(concert.getStatus())
                .ticketTiers(tiers)
                .venue(venue)
                .organizerId(organizerId)
                .organizerName(organizerName)
                .organizerRating(organizerRating)
                .build();
    }

    private BigDecimal getStartingPrice(UUID concertId) {
        return ticketTierRepository
                .findByConcertIdAndIsActiveTrue(concertId)
                .stream()
                .map(TicketTier::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    @Cacheable(value = "concerts",key="'upcoming:' + #pageable.pageNumber")
    public PagedResponse<ConcertSummaryResponse> getUpcomingConcerts(Pageable pageable){
        Page<ConcertSummaryResponse> page=concertRepository.findUpcomingConcerts(
                LocalDateTime.now(),
                pageable
        );
        return PageResponseBuilder.from(page);
    }
   @Cacheable(value = "concerts",key = "'city:' + #city + ':' + #pageable.pageNumber")
    public PagedResponse<ConcertSummaryResponse> getConcertByCity(String city, Pageable pageable) {
        Page<ConcertSummaryResponse>page=concertRepository.findByCityProjection(
                city,
                ConcertStatus.PUBLISHED,
                pageable
        );
        return PageResponseBuilder.from(page);

    }
    @Cacheable(value = "concerts",key = "'search:' + #query + ':' + #pageable.pageNumber")
    public  PagedResponse<ConcertSummaryResponse> searchConcerts(String query,Pageable pageable){
        Page<ConcertSummaryResponse>page=concertRepository.findBySearchProjection(
                query,
                ConcertStatus.PUBLISHED,
                pageable
        );
        return PageResponseBuilder.from(page);
    }
    @Cacheable(value = "concerts",key="#concertId")
    public ConcertResponse getConcertDetail(UUID concertId){
       Concert concert=getConcertEntityById(concertId);
        return buildConcertResponse(concert);

    }
    @Cacheable(value = "concerts", key = "'featured'")
    public List<ConcertSummaryResponse> getFeaturedConcerts() {

        return concertRepository.findFeaturedProjection(ConcertStatus.PUBLISHED,LocalDateTime.now());
    }
    //For category filtering
    public PagedResponse<ConcertSummaryResponse> getByCategory(UUID categoryId, Pageable pageable){
        Page<ConcertSummaryResponse> page=concertRepository.findByCategoryProjection(categoryId,LocalDateTime.now(),pageable);
        return PageResponseBuilder.from(page);
    }

    //For location discovery
    public PagedResponse<ConcertSummaryResponse> getNearbyEvents(
            double userLat,
            double userLng,
            Double radiusKm,
            Pageable pageable) {

        if (radiusKm==null) {
            Page<ConcertSummaryResponse>page= concertRepository.findUpcomingConcerts(
                    LocalDateTime.now(), pageable);
            return PageResponseBuilder.from(page);
        }
        int offset=(int)pageable.getOffset();
        int limit=pageable.getPageSize();

        List<Object[]> raw=concertRepository.findNearbyRaw(userLat,userLng,radiusKm,limit,offset);
        long total=concertRepository.countNearby(userLat,userLng,radiusKm);

        if (raw.isEmpty()) {
            Page<ConcertSummaryResponse> emptyPage=new PageImpl<>(Collections.emptyList(),pageable,
                            0);
            return PageResponseBuilder.from(emptyPage);
        }
        //Build id into distance map
        Map<UUID,Double> distanceMap=new LinkedHashMap<>();
        List<UUID> orderedIds=new ArrayList<>();
        for (Object[] row:raw) {
            UUID id=UUID.fromString((String)row[0]);
            Double dist=((Number)row[1]).doubleValue();
            distanceMap.put(id,dist);
            orderedIds.add(id);
        }

        List<Concert>concerts=concertRepository.findAllById(orderedIds);
        Map<UUID,Concert> concertMap=concerts.stream()
                .collect(Collectors.toMap(Concert::getId,c -> c));

        // Build responses with distance, preserving sort order from Haversine
        List<ConcertSummaryResponse>results=orderedIds.stream()
                .filter(concertMap::containsKey)
                .map(id->{
                    Concert c=concertMap.get(id);
                    BigDecimal price=getStartingPrice(c.getId());
                    double dist=distanceMap.get(id);
                    return new ConcertSummaryResponse(
                            c.getId(),
                            c.getTitle(),
                            c.getArtistName(),
                            c.getBannerImageUrl(),
                            c.getVenue().getName(),
                            c.getVenue().getCity(),
                            c.getConcertDate(),
                            c.getSaleStartTime(),
                            c.getStatus(),
                            c.isFeatured(),
                            c.isRequiresPreRegistration(),
                            price,
                            c.getCategory() != null ? c.getCategory().getName() : null,
                            Math.round(dist * 10.0) / 10.0,
                            c.getOrganizer() != null ? c.getOrganizer().getId() : null,
                            c.getOrganizer() != null ? c.getOrganizer().getName() : null
                    );
                })
                .collect(Collectors.toList());

        Page<ConcertSummaryResponse> page = new PageImpl<>(results,pageable,total);
        return PageResponseBuilder.from(page);
    }
    // ORGANIZER SECTION

    @Transactional
    @CacheEvict(value = {"concerts", "concert"}, allEntries = true)
    public ConcertResponse createConcert(ConcertRequest request, User organizer) {

        Venue venue = venueRepository.findById(request.getVenueId()).orElseThrow(() -> new ResourceNotFoundException(
                        "Venue not found: " + request.getVenueId()));

        EventCategory category = eventCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        Concert concert = Concert.builder()
                .title(request.getTitle())
                .artistName(request.getArtistName())
                .description(request.getDescription())
                .venue(venue)
                .organizer(organizer)
                .category(category)
                .concertDate(request.getConcertDate())
                .doorsOpenTime(request.getDoorsOpenTime())
                .saleStartTime(request.getSaleStartTime())
                .saleEndTime(request.getSaleEndTime())
                .bannerImageUrl(request.getBannerImageUrl())
                .status(ConcertStatus.DRAFT)
                .requiresPreRegistration(request.isRequiresPreRegistration())
                .preRegistrationStart(request.getPreRegistrationStart())
                .preRegistrationEnd(request.getPreRegistrationEnd())
                .isFeatured(false)
                .build();

        Concert saved = concertRepository.save(concert);
        log.info("Concert created: {} by: {}",saved.getId(),organizer.getEmail());
        return buildConcertResponse(saved);
    }

    @Transactional
    @CacheEvict(value = {"concerts", "concert"}, allEntries = true)
    public TicketTierResponse addTicketTier(UUID concertId, CreateTicketTierRequest request, User organizer) {

        Concert concert = getConcertEntityById(concertId);

        if (!concert.getOrganizer().getId().equals(organizer.getId())) {
            throw new AuthException("You are not the organizer of this concert");
        }
        int finalQuantity;
        if (request.getSectionType() == SectionType.ASSIGNED) {
            if (request.getRowCount() == null||request.getSeatsPerRow() == null) {
                throw new BookingException("Assigned tiers require rowCount and seatsPerRow");
            }
            finalQuantity=request.getRowCount()*request.getSeatsPerRow();
        } else {
            if (request.getTotalQuantity() == null) {
                throw new BookingException("Total quantity is required");
            }
            finalQuantity=request.getTotalQuantity();
        }
        VenueSection section = venueSectionRepository.findById(request.getSectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));

        TicketTier tier = TicketTier.builder()
                .concert(concert)
                .section(section)
                .tierName(request.getTierName())
                .price(request.getPrice())
                .totalQuantity(finalQuantity)
                .availableQuantity(finalQuantity)
                .maxPerUser(request.getMaxPerUser())
                .saleStart(request.getSaleStart())
                .saleEnd(request.getSaleEnd())
                .lockTtlMinutes(request.getLockTtlMinutes())
                .tierStatus(TierStatus.UPCOMING)
                .isActive(true)
                .build();

        TicketTier saved = ticketTierRepository.save(tier);
        if (section.getSectionType() == SectionType.ASSIGNED) {
            int rows = request.getRowCount();
            int seatsPerRow = request.getSeatsPerRow();
            for (int i = 0; i < rows; i++) {
                String rowLabel = String.valueOf((char) ('A' + i));
                for (int j = 1; j <= seatsPerRow; j++) {
                    SeatInventory seat = new SeatInventory();
                    seat.setTier(saved);
                    seat.setRowLabel(rowLabel);
                    seat.setSeatNumber(String.valueOf(j));
                    seat.setStatus(SeatStatus.AVAILABLE);
                    seatInventoryRepository.save(seat);
                }
            }
        }
        log.info("Tier {} added and physical seats generated for concert {}", saved.getTierName(), concertId);
        return concertMapper.toTierResponse(saved);
    }

    @Transactional
    @CacheEvict(value = {"concerts", "concert"},allEntries = true)
    public ConcertResponse publishConcert(UUID concertId,User organizer) {

        Concert concert = getConcertEntityById(concertId);

        if (!concert.getOrganizer().getId().equals(organizer.getId())) {
            throw new AuthException("You are not the organizer of this concert");
        }
        List<TicketTier> tiers = ticketTierRepository.findByConcertIdAndIsActiveTrue(concertId);
        if (tiers.isEmpty()) {
            throw new BookingException(
                    "Cannot publish concert without ticket tiers");
        }

        concert.setStatus(ConcertStatus.PUBLISHED);
        tiers.forEach(t -> t.setTierStatus(TierStatus.ACTIVE));
        ticketTierRepository.saveAll(tiers);

        Concert saved = concertRepository.save(concert);
        log.info("Concert published: {}", concertId);
        return buildConcertResponse(saved);
    }

    public PagedResponse<ConcertSummaryResponse> getOrganizerConcerts(
            UUID organizerId, Pageable pageable){
        Page<ConcertSummaryResponse>page=concertRepository.findOrganizerConcertsProjection(organizerId,pageable);
        return PageResponseBuilder.from(page);
    }

    public Concert getConcertEntityById(UUID concertId) {
        return concertRepository.findByIdWithVenue(concertId)
                .orElseThrow(() -> new ResourceNotFoundException("Concert not found: " + concertId));
    }

    @Transactional
    @CacheEvict(value = {"concerts", "concert"}, allEntries = true)
    public ConcertResponse cancelConcert(UUID concertId, User user, String reason) {
        Concert concert = getConcertEntityById(concertId);

        boolean isAdmin = user.getRole() == UserRole.ADMIN;
        boolean isOrganizer = concert.getOrganizer().getId().equals(user.getId());

        if (!isAdmin && !isOrganizer) {
            throw new AuthException("Not authorized to cancel this concert");
        }

        concert.setStatus(ConcertStatus.CANCELLED);
        concertRepository.save(concert);

        List<TicketTier> tiers = ticketTierRepository.findByConcertIdAndIsActiveTrue(concertId);
        tiers.forEach(t -> t.setTierStatus(TierStatus.CANCELLED));
        ticketTierRepository.saveAll(tiers);

        log.info("Concert cancelled: {} by: {}",concertId,user.getEmail());
        return buildConcertResponse(concert);
    }

    @Transactional
    @CacheEvict(value ={"concerts","concert"},allEntries = true)
    public ConcertResponse postponeConcert(UUID concertId,LocalDateTime newDate, User user) {
        Concert concert = getConcertEntityById(concertId);

        boolean isAdmin = user.getRole() == UserRole.ADMIN;
        boolean isOrganizer = concert.getOrganizer().getId().equals(user.getId());

        if (!isAdmin && !isOrganizer) {
            throw new AuthException("Not authorized to postpone this concert");
        }

        concert.setConcertDate(newDate);
        concert.setStatus(ConcertStatus.POSTPONED);
        concertRepository.save(concert);

        log.info("Concert postponed: {} new date: {}", concertId, newDate);
        return buildConcertResponse(concert);
    }

    @Transactional
    @CacheEvict(value = {"concerts", "concert"}, allEntries = true)
    public ConcertResponse setFeatured(UUID concertId, boolean featured) {
        Concert concert = getConcertEntityById(concertId);
        concert.setFeatured(featured);
        concertRepository.save(concert);
        log.info("Concert {} featured: {}", concertId, featured);
        return buildConcertResponse(concert);
    }

    public PagedResponse<ConcertSummaryResponse> getAllConcerts(Pageable pageable) {
        Page<ConcertSummaryResponse> page = concertRepository.findAllConcertsProjection(pageable);
        return PageResponseBuilder.from(page);
    }

}
