package com.concertbooking.concert_booking.user.service;

import com.concertbooking.concert_booking.common.enums.UserRole;
import com.concertbooking.concert_booking.common.exception.AuthException;
import com.concertbooking.concert_booking.common.exception.ResourceNotFoundException;
import com.concertbooking.concert_booking.user.dto.UpdateProfileRequest;
import com.concertbooking.concert_booking.user.dto.UserProfileResponse;
import com.concertbooking.concert_booking.user.entity.User;
import com.concertbooking.concert_booking.user.mapper.UserMapper;
import com.concertbooking.concert_booking.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    // GET PROFILE-
    public UserProfileResponse getProfile(User user){
        return userMapper.toResponse(user);
    }
    // UPDATE PROFILE-
    @Transactional
    public  UserProfileResponse updateProfile(User user, UpdateProfileRequest request) {
        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName());
        }

        if (request.getPhone() != null && !request.getPhone().isBlank()) {

            if (userRepository.existsByPhone(request.getPhone())
                    && !request.getPhone().equals(user.getPhone())) {
                throw new AuthException("Phone number already in use");

            }
            user.setPhone(request.getPhone());
        }
            User saved = userRepository.save(user);
            log.info("Profile updated for user: {}", user.getEmail());
            return userMapper.toResponse(saved);

        }
        //CHANGE PASSWORD-
    public void changePassword(User user, String currentPassword, String newPassword){
        if(user.getPassword()==null){
            throw new AuthException("Cannot change passwords for OAuth accounts");
        }


        if(!passwordEncoder.matches(currentPassword, user.getPassword())){
            throw new AuthException("The current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password has been successfully changed, {}",user.getEmail());

    }
    //DEACTIVATE ACCOUNT-
    @Transactional
    public void deactivateAccount(User user,String password){
        if(user.getPassword()!=null && !passwordEncoder.matches(password, user.getPassword())){
            throw new AuthException("The current password is incorrect");
        }
        user.setActive(false);
        userRepository.save(user);
        log.info("Account deactivated for user: {}",user.getEmail());
    }
    // ADMIN-> GET USER BY THEIR ID-
    public  UserProfileResponse getUserById(UUID userId){
        User user=userRepository.findById(userId).orElseThrow(()->new ResourceNotFoundException(
                "User not found: "+userId
        ));
        return userMapper.toResponse(user);
    }
    public Page<UserProfileResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(userMapper::toResponse);
    }

    @Transactional
    public void banUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        user.setActive(false);
        userRepository.save(user);
        log.info("User banned: {}", userId);
    }

    @Transactional
    public void unbanUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        user.setActive(true);
        userRepository.save(user);
        log.info("User unbanned: {}", userId);
    }

    @Transactional
    public void changeRole(UUID userId, UserRole role) {
        if (role == UserRole.ADMIN) {
            throw new AuthException("Cannot assign ADMIN role via API");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        user.setRole(role);
        userRepository.save(user);
        log.info("Role changed for user: {} to: {}", userId, role);
    }
    }

