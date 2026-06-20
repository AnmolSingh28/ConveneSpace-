package com.concertbooking.concert_booking.concert.service;

import com.concertbooking.concert_booking.concert.dto.EventCategoryResponse;
import com.concertbooking.concert_booking.concert.entity.EventCategory;
import com.concertbooking.concert_booking.concert.mapper.EventCategoryMapper;
import com.concertbooking.concert_booking.concert.repository.EventCategoryRepository;
import com.concertbooking.concert_booking.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
@Service
@RequiredArgsConstructor
public class EventCategoryService {
    private final EventCategoryRepository categoryRepository;
    private final EventCategoryMapper mapper;
    public List<EventCategoryResponse> getActiveCategories() {
        return categoryRepository.findByActiveTrue()
                .stream()
                .map(mapper::toResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public EventCategory createOrGet(String name) {
        return categoryRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> categoryRepository.save(
                        EventCategory.builder().name(name).active(true).build()
                ));
    }

    @Transactional
    public void deactivate(UUID id) {
        EventCategory cat = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        cat.setActive(false);
        categoryRepository.save(cat);
    }
}
