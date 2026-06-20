package com.concertbooking.concert_booking.review.mapper;

import com.concertbooking.concert_booking.review.dto.OrganizerReviewResponse;
import com.concertbooking.concert_booking.review.entity.OrganizerReview;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrganizerReviewMapper {
    @Mapping(target = "reviewId", source = "id")
    @Mapping(target = "reviewerName", source = "reviewer.name")
    @Mapping(target = "concertTitle", source = "concert.title")
    OrganizerReviewResponse toResponse(OrganizerReview review);
}
