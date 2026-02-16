package com.mzika.service;

import com.mzika.model.dto.SpotifyTokenResponse;
import com.mzika.model.dto.SpotifyUserInfo;
import com.mzika.model.entity.User;
import com.mzika.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final SpotifyService spotifyService;

    @Transactional
    public User createOrUpdateUser(SpotifyUserInfo spotifyUserInfo, String accessToken, String refreshToken)
    {

        Optional<User> existingUser = userRepository.findBySpotifyId(spotifyUserInfo.getId());
        User user;

        if(existingUser.isPresent())
        {
            user = existingUser.get();
            log.info("Updating existing user: {}", spotifyUserInfo.getId());
        }else{
            user = new User();
            user.setSpotifyId(spotifyUserInfo.getId());
            log.info("Creating new user: {}", spotifyUserInfo.getId());
        }

        user.setEmail(spotifyUserInfo.getEmail());
        user.setDisplayName(spotifyUserInfo.getDisplayName());
        user.setName(spotifyUserInfo.getDisplayName());
        user.setAccessToken(accessToken);
        user.setRefreshToken(refreshToken);

        user.setTokenExpiresAt(LocalDateTime.now().plusHours(1));

        return  userRepository.save(user);
    }

    public Optional<User> getUserBySpotifyId(String spotifyId)
    {
        return userRepository.findBySpotifyId(spotifyId);
    }

    public boolean isTokenExpired(User user)
    {
        return user.getTokenExpiresAt() != null &&
                user.getTokenExpiresAt().isBefore(LocalDateTime.now());
    }
    @Transactional
    public User refreshUserToken(User user) {
        try {
            if (user.getRefreshToken() == null) {
                log.warn("No refresh token available for user: {}", user.getSpotifyId());
                throw new RuntimeException("No refresh token available");
            }

            log.info("Refreshing token for user: {}", user.getSpotifyId());

            SpotifyTokenResponse tokenResponse = spotifyService.refreshAccessToken(
                    user.getRefreshToken()
            );

            // Update tokens
            user.setAccessToken(tokenResponse.getAccessToken());

            // Spotify only sends new refresh token sometimes
            if (tokenResponse.getRefreshToken() != null) {
                user.setRefreshToken(tokenResponse.getRefreshToken());
            }

            // Update expiry time
            user.setTokenExpiresAt(LocalDateTime.now().plusSeconds(
                    tokenResponse.getExpiresIn() != null ? tokenResponse.getExpiresIn() : 3600
            ));

            return userRepository.save(user);

        } catch (Exception e) {
            log.error("Error refreshing token for user {}: {}", user.getSpotifyId(), e.getMessage());
            throw new RuntimeException("Failed to refresh token", e);
        }
    }

    public User getValidUser(String spotifyId) {
        User user = userRepository.findBySpotifyId(spotifyId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Auto-refresh if token is expired
        if (isTokenExpired(user)) {
            log.info("Token expired for user: {}, refreshing...", spotifyId);
            user = refreshUserToken(user);
        }

        return user;
    }
}
