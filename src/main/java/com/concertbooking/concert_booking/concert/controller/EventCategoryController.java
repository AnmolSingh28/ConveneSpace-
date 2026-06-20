package com.concertbooking.concert_booking.concert.controller;

import com.concertbooking.concert_booking.common.response.ApiResponse;
import com.concertbooking.concert_booking.concert.dto.EventCategoryResponse;
import com.concertbooking.concert_booking.concert.mapper.EventCategoryMapper;
import com.concertbooking.concert_booking.concert.service.EventCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class EventCategoryController {
    private final EventCategoryService categoryService;
    private final EventCategoryMapper categoryMapper;

    @GetMapping
    @Operation(
            summary = "Get all active event categories",
            description = "Returns all active event categories available for event creation and filtering."
    )
    public ResponseEntity<ApiResponse<List<EventCategoryResponse>>>getAll(){
        return ResponseEntity.ok(ApiResponse.success(categoryService.getActiveCategories(),"Categories fetched"));
    }
    @PostMapping
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(
            summary = "Create a new event category",
            description = "Creates a new category if it does not already exist. If the category already exists, the existing category is returned."
    )
    public ResponseEntity<ApiResponse<EventCategoryResponse>>create(@RequestParam String name){
        return ResponseEntity.ok(ApiResponse.success(categoryMapper.toResponse(categoryService.createOrGet(name)),"Done"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Deactivate event category",
            description = "Marks a category as inactive. Only administrators can perform this action."
    )
    public ResponseEntity<ApiResponse<Void>>deactivate(@PathVariable UUID id){
        categoryService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success(null,"Deactivated"));
    }
}
