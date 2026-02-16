package com.mzika.controller;

import com.mzika.model.dto.FavoriteTrackDTO;
import com.mzika.model.dto.TrackDTO;
import com.mzika.model.entity.User;
import com.mzika.service.FavoriteService;
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

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
@Slf4j
public class FavoritesController {

    private final FavoriteService favoriteService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<?> getFavorites(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }

        try {
            User user = userService.getValidUser(principal.getAttribute("id"));
            List<FavoriteTrackDTO> favorites = favoriteService.getUserFavorites(user);
            return ResponseEntity.ok(favorites);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get favorites"));
        }
    }

    @PostMapping
    public ResponseEntity<?> addFavorite(
            @RequestBody TrackDTO track,
            @AuthenticationPrincipal OAuth2User principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }

        try {
            User user = userService.getValidUser(principal.getAttribute("id"));
            FavoriteTrackDTO saved = favoriteService.addFavorite(user, track);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to add favorite"));
        }
    }

    @DeleteMapping("/{trackId}")
    public ResponseEntity<?> removeFavorite(
            @PathVariable String trackId,
            @AuthenticationPrincipal OAuth2User principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }

        try {
            User user = userService.getValidUser(principal.getAttribute("id"));
            favoriteService.removeFavorite(user, trackId);
            return ResponseEntity.ok(Map.of("message", "Removed from favorites"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to remove favorite"));
        }
    }

    @GetMapping("/check/{trackId}")
    public ResponseEntity<?> checkFavorite(
            @PathVariable String trackId,
            @AuthenticationPrincipal OAuth2User principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }

        try {
            User user = userService.getValidUser(principal.getAttribute("id"));
            boolean isFav = favoriteService.isFavorite(user, trackId);
            return ResponseEntity.ok(Map.of("isFavorite", isFav));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to check favorite"));
        }
    }
}