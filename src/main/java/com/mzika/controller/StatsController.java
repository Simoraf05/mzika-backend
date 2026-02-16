package com.mzika.controller;

import com.mzika.model.entity.User;
import com.mzika.repository.FavoriteTrackRepository;
import com.mzika.service.SearchHistoryService;
import com.mzika.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final UserService userService;
    private final FavoriteTrackRepository favoriteTrackRepository;
    private final SearchHistoryService searchHistoryService;

    @GetMapping
    public ResponseEntity<?> getUserStats(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }

        try {
            User user = userService.getValidUser(principal.getAttribute("id"));

            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("totalFavorites", favoriteTrackRepository.countByUser(user));
            stats.put("totalSearches", searchHistoryService.getStats(user).get("totalSearches"));
            stats.put("topQueries", searchHistoryService.getStats(user).get("topQueries"));
            stats.put("memberSince", user.getCreatedAt().toString());
            stats.put("spotifyPlan", principal.getAttribute("product"));
            stats.put("country", principal.getAttribute("country"));

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get stats"));
        }
    }
}