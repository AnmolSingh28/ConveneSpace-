package com.concertbooking.concert_booking.review.entity;

import com.concertbooking.concert_booking.concert.entity.Concert;
import com.concertbooking.concert_booking.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "organizer_reviews", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_concert_review",columnNames={"reviewer_id","concert_id"})
},indexes = {
        @Index(name = "idx_review_organizer",columnList = "organizer_id"),
        @Index(name = "idx_review_concert",columnList = "concert_id"),
        @Index(name = "idx_review_reviewer",columnList = "reviewer_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"organizer", "reviewer", "concert"})
public class OrganizerReview {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="reviewer_id", nullable=false)
    private User reviewer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="concert_id",nullable=false)
    private Concert concert;

    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private Integer rating;

    @NotBlank
    @Column(name="review_text",columnDefinition ="TEXT")
    private String reviewText;

    @CreationTimestamp
    @Column(name="created_at",updatable=false)
    private LocalDateTime createdAt;

    @Column(name="would_attend_again")
    private Boolean wouldAttendAgain;
}
