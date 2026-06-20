package com.concertbooking.concert_booking.user.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import com.concertbooking.concert_booking.common.enums.UserRole;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name="users",indexes = {
        @Index(name="idx_user_email",columnList = "email"), //we use these fields as these help to traverse the db faster instead of checking every field we check only these two
        // these fields will be accessed more frequently so they must be indexed for faster accessing
        @Index(name="idx_user_phone",columnList = "phone")

})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

@EqualsAndHashCode(of="id")
@ToString(exclude ="password")

public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false,unique = true)
    private String email;

    @Column
    private String password;

    @Column(unique = true)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)

    private UserRole role;

    @Column(name="is_email_verification",nullable=false)
    private boolean emailVerified=false;

    @Column(name="is_phone_verified",nullable = false)
    private boolean phoneVerification=false;

    @Column(name="oauth_provider")
    private String oauthProvider;

    @Column(name="oauth_id")
    private String oauthId;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;



    @Override

    //The below makes a string LIKE :- ROLE_ADMIN which helps spring to decide what endpoints it can access
    public Collection<? extends GrantedAuthority> getAuthorities(){
        return List.of( new SimpleGrantedAuthority("ROLE_"+role.name()));
    }
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active && emailVerified;
    }


}
