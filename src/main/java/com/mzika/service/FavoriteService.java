package com.mzika.service;

import com.mzika.model.dto.FavoriteTrackDTO;
import com.mzika.model.dto.TrackDTO;
import com.mzika.model.entity.FavoriteTrack;
import com.mzika.model.entity.User;
import com.mzika.repository.FavoriteTrackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FavoriteService {

    private final FavoriteTrackRepository favoriteTrackRepository;

    public List<FavoriteTrackDTO> getUserFavorites(User user) {
        return favoriteTrackRepository.findByUserOrderBySavedAtDesc(user)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public FavoriteTrackDTO addFavorite(User user, TrackDTO track) {
        // Check if already favorited
        if (favoriteTrackRepository.existsByUserAndTrackId(user, track.getId())) {
            return toDTO(favoriteTrackRepository
                    .findByUserAndTrackId(user, track.getId()).get());
        }

        String artistName = track.getArtists() != null && !track.getArtists().isEmpty()
                ? track.getArtists().stream()
                .map(a -> a.getName())
                .collect(Collectors.joining(", "))
                : "Unknown Artist";

        String albumImageUrl = track.getAlbum() != null &&
                track.getAlbum().getImages() != null &&
                !track.getAlbum().getImages().isEmpty()
                ? track.getAlbum().getImages().get(0).getUrl()
                : null;

        FavoriteTrack favorite = FavoriteTrack.builder()
                .user(user)
                .trackId(track.getId())
                .trackName(track.getName())
                .artistName(artistName)
                .albumName(track.getAlbum() != null ? track.getAlbum().getName() : null)
                .albumImageUrl(albumImageUrl)
                .durationMs(track.getDurationMs())
                .popularity(track.getPopularity())
                .explicit(track.getExplicit())
                .build();

        FavoriteTrack saved = favoriteTrackRepository.save(favorite);
        log.info("Added favorite track: {} for user: {}", track.getName(), user.getSpotifyId());
        return toDTO(saved);
    }

    @Transactional
    public void removeFavorite(User user, String trackId) {
        favoriteTrackRepository.deleteByUserAndTrackId(user, trackId);
        log.info("Removed favorite track: {} for user: {}", trackId, user.getSpotifyId());
    }

    public boolean isFavorite(User user, String trackId) {
        return favoriteTrackRepository.existsByUserAndTrackId(user, trackId);
    }

    private FavoriteTrackDTO toDTO(FavoriteTrack favorite) {
        return FavoriteTrackDTO.builder()
                .id(favorite.getId())
                .trackId(favorite.getTrackId())
                .trackName(favorite.getTrackName())
                .artistName(favorite.getArtistName())
                .albumName(favorite.getAlbumName())
                .albumImageUrl(favorite.getAlbumImageUrl())
                .durationMs(favorite.getDurationMs())
                .popularity(favorite.getPopularity())
                .explicit(favorite.getExplicit())
                .savedAt(favorite.getSavedAt())
                .build();
    }
}