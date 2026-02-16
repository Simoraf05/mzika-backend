package com.mzika.controller;

import com.mzika.model.dto.MoodRequestDTO;
import com.mzika.model.dto.PlaylistDTO;
import com.mzika.model.dto.RecommendationRequestDTO;
import com.mzika.model.dto.SearchResultDTO;
import com.mzika.model.entity.User;
import com.mzika.service.RecommendationService;
import com.mzika.service.SearchHistoryService;
import com.mzika.service.SpotifyService;
import com.mzika.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/music")
@RequiredArgsConstructor
@Slf4j
public class MusicController {
    private final SpotifyService spotifyService;
    private final UserService userService;
    private final RecommendationService recommendationService;
    private final SearchHistoryService searchHistoryService;

    @GetMapping("/search")
    public ResponseEntity<?> searchTracks(
            @RequestParam String query,
            @RequestParam(required = false, defaultValue = "20") Integer limit,
            @RequestParam(required = false, defaultValue = "0") Integer offset,
            @AuthenticationPrincipal OAuth2User principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }

        try {
            String spotifyId = principal.getAttribute("id");
            User user = userService.getValidUser(spotifyId);

            SearchResultDTO results = spotifyService.searchTracks(query, user.getAccessToken(), limit, offset);

            // Save to search history
            searchHistoryService.saveSearch(user, query, results.getTotal());

            log.info("Found {} tracks for query: {}", results.getTotal(), query);
            return ResponseEntity.ok(results);

        } catch (Exception e) {
            log.error("Error searching tracks: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to search tracks", "message", e.getMessage()));
        }
    }

    @PostMapping("/recommendations/mood")
    public ResponseEntity<?> getRecommendationsByMood(
            @RequestBody MoodRequestDTO request,
            @AuthenticationPrincipal OAuth2User principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }

        try {
            String spotifyId = principal.getAttribute("id");
            User user = userService.getValidUser(spotifyId);

            SearchResultDTO results = recommendationService
                    .getRecommendationsByMood(request, user.getAccessToken());

            log.info("Got {} mood-based recommendations", results.getTracks().size());
            return ResponseEntity.ok(results);

        } catch (Exception e) {
            log.error("Error getting recommendations: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get recommendations"));
        }
    }

    @GetMapping("/recommendations/similar")
    public ResponseEntity<?> getSimilarTracks(
            @RequestParam String trackName,
            @RequestParam String artistName,
            @RequestParam(required = false, defaultValue = "20") Integer limit,
            @AuthenticationPrincipal OAuth2User principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }

        try {
            String spotifyId = principal.getAttribute("id");
            User user = userService.getValidUser(spotifyId);

            SearchResultDTO results = recommendationService
                    .getSimilarTracks(trackName, artistName, user.getAccessToken(), limit);

            return ResponseEntity.ok(results);

        } catch (Exception e) {
            log.error("Error getting similar tracks: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get similar tracks"));
        }
    }

    @GetMapping("/playlists")
    public ResponseEntity<?> getUserPlaylists(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }

        try {
            User user = userService.getValidUser(principal.getAttribute("id"));
            List<PlaylistDTO> playlists = spotifyService.getUserPlaylists(user.getAccessToken());
            return ResponseEntity.ok(playlists);
        } catch (Exception e) {
            log.error("Error getting playlists: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get playlists"));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getSearchHistory(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }

        try {
            User user = userService.getValidUser(principal.getAttribute("id"));
            List<Map<String, Object>> history = searchHistoryService.getRecentSearches(user)
                    .stream()
                    .map(h -> Map.<String, Object>of(
                            "id", h.getId(),
                            "query", h.getQuery(),
                            "resultsCount", h.getResultsCount(),
                            "searchedAt", h.getSearchedAt().toString()
                    ))
                    .toList();
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get history"));
        }
    }

    @DeleteMapping("/history")
    public ResponseEntity<?> clearHistory(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }

        try {
            User user = userService.getValidUser(principal.getAttribute("id"));
            searchHistoryService.clearHistory(user);
            return ResponseEntity.ok(Map.of("message", "History cleared"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to clear history"));
        }
    }

    @PostMapping("/playlists/create")
    public ResponseEntity<?> createPlaylist(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal OAuth2User principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }

        try {
            User user = userService.getValidUser(principal.getAttribute("id"));
            String spotifyUserId = principal.getAttribute("id");

            String name = (String) request.get("name");
            String description = (String) request.getOrDefault("description", "Created by Mzika");
            List<String> trackUris = (List<String>) request.get("trackUris");

            log.info("Creating playlist '{}' with {} tracks for user: {}",
                    name, trackUris != null ? trackUris.size() : 0, spotifyUserId);

            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Playlist name is required"));
            }

            if (trackUris == null || trackUris.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "No tracks provided"));
            }

            // Spotify allows max 100 tracks per request
            List<String> limitedUris = trackUris.stream().limit(100).toList();

            String playlistId = spotifyService.createPlaylist(
                    spotifyUserId, name.trim(), description, user.getAccessToken()
            );

            spotifyService.addTracksToPlaylist(playlistId, limitedUris, user.getAccessToken());

            return ResponseEntity.ok(Map.of(
                    "message", "Playlist created successfully!",
                    "playlistId", playlistId,
                    "playlistUrl", "https://open.spotify.com/playlist/" + playlistId
            ));

        } catch (Exception e) {
            log.error("Error creating playlist: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
