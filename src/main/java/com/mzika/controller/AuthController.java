package com.mzika.controller;

import com.mzika.model.entity.User;
import com.mzika.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"}, allowCredentials = "true")
public class AuthController {

    private final UserService userService;

    @GetMapping("/user")
    public Map<String, Object> getCurrentUser(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return Map.of("authenticated", false);
        }

        String spotifyId = principal.getAttribute("id");
        Optional<User> user = userService.getUserBySpotifyId(spotifyId);

        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", true);
        response.put("spotifyId", spotifyId);
        response.put("displayName", principal.getAttribute("display_name"));
        response.put("email", principal.getAttribute("email"));

        user.ifPresent(u -> {
            response.put("savedInDb", true);
            response.put("tokenExpired", userService.isTokenExpired(u));
        });

        return response;
    }

    @PostMapping("/logout")
    public Map<String, String> logout(HttpSession session) {
        session.invalidate();
        return Map.of("message", "Logged out successfully");
    }

    @GetMapping("/success")
    public Map<String, Object> loginSuccess(@AuthenticationPrincipal OAuth2User principal) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Login successful! User saved to database.");
        response.put("user", principal.getAttributes());
        response.put("savedInDatabase", true);
        return response;
    }
}