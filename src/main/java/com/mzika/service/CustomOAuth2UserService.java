package com.mzika.service;

import com.mzika.model.dto.SpotifyUserInfo;
import com.mzika.model.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserService userService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // Load user info from Spotify
        OAuth2User oauth2User = super.loadUser(userRequest);

        // LOG ALL ATTRIBUTES TO SEE WHAT WE GET
        log.info("Spotify user attributes: {}", oauth2User.getAttributes());

        // Extract tokens
        String accessToken = userRequest.getAccessToken().getTokenValue();

        // Get refresh token (might be null if not provided by Spotify)
        String refreshToken = userRequest.getAccessToken().getTokenValue(); // Temporary: use access token

        // Extract user info from OAuth2User
        SpotifyUserInfo spotifyUserInfo = extractSpotifyUserInfo(oauth2User);

        log.info("Extracted SpotifyUserInfo: id={}, displayName={}, email={}",
                spotifyUserInfo.getId(), spotifyUserInfo.getDisplayName(), spotifyUserInfo.getEmail());

        // Save or update user in database
        User user = userService.createOrUpdateUser(spotifyUserInfo, accessToken, refreshToken);

        log.info("User authenticated: {} ({})", user.getDisplayName(), user.getSpotifyId());

        return oauth2User;
    }

    private SpotifyUserInfo extractSpotifyUserInfo(OAuth2User oauth2User) {
        Map<String, Object> attributes = oauth2User.getAttributes();

        // Extract image URL (first image if available)
        String imageUrl = null;
        if (attributes.containsKey("images")) {
            List<Map<String, Object>> images = (List<Map<String, Object>>) attributes.get("images");
            if (images != null && !images.isEmpty()) {
                imageUrl = (String) images.get(0).get("url");
            }
        }

        // Get display_name with fallback to id if null
        String displayName = (String) attributes.get("display_name");
        String id = (String) attributes.get("id");

        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = id; // Use Spotify ID as fallback
            log.warn("display_name is null, using id as fallback: {}", id);
        }

        return SpotifyUserInfo.builder()
                .id(id)
                .displayName(displayName)
                .email((String) attributes.get("email"))
                .country((String) attributes.get("country"))
                .product((String) attributes.get("product"))
                .imageUrl(imageUrl)
                .build();
    }
}