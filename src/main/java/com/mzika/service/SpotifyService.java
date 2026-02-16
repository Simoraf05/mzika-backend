package com.mzika.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mzika.model.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpotifyService {

    @Value("${spotify.api.base-url}")
    private String spotifyApiBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String getUserProfile(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                spotifyApiBaseUrl + "/me",
                HttpMethod.GET,
                entity,
                String.class
        );

        return response.getBody();
    }

    public SearchResultDTO searchTracks(String query, String accessToken, Integer limit, Integer offset) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Build URL with query parameters
            // New code - use this instead
            String url = spotifyApiBaseUrl + "/search?q=" +
                    java.net.URLEncoder.encode(query, "UTF-8") +
                    "&type=track" +
                    "&limit=" + (limit != null ? limit : 20) +
                    "&offset=" + (offset != null ? offset : 0);

            log.info("Searching Spotify for: {}", query);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            // Parse JSON response
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode tracksNode = root.path("tracks");

            List<TrackDTO> tracks = new ArrayList<>();
            JsonNode itemsNode = tracksNode.path("items");

            if (itemsNode.isArray()) {
                for (JsonNode trackNode : itemsNode) {
                    tracks.add(parseTrack(trackNode));
                }
            }

            return SearchResultDTO.builder()
                    .tracks(tracks)
                    .total(tracksNode.path("total").asInt())
                    .limit(tracksNode.path("limit").asInt())
                    .offset(tracksNode.path("offset").asInt())
                    .build();

        } catch (Exception e) {
            log.error("Error searching tracks: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to search tracks", e);
        }
    }

    private TrackDTO parseTrack(JsonNode trackNode) {
        // Parse artists
        List<ArtistDTO> artists = new ArrayList<>();
        JsonNode artistsNode = trackNode.path("artists");
        if (artistsNode.isArray()) {
            for (JsonNode artistNode : artistsNode) {
                artists.add(ArtistDTO.builder()
                        .id(artistNode.path("id").asText())
                        .name(artistNode.path("name").asText())
                        .uri(artistNode.path("uri").asText())
                        .build());
            }
        }

        // Parse album
        JsonNode albumNode = trackNode.path("album");
        List<ImageDTO> images = new ArrayList<>();
        JsonNode imagesNode = albumNode.path("images");
        if (imagesNode.isArray()) {
            for (JsonNode imageNode : imagesNode) {
                ImageDTO image = new ImageDTO();
                image.setHeight(imageNode.path("height").asInt());
                image.setWidth(imageNode.path("width").asInt());
                image.setUrl(imageNode.path("url").asText());
                images.add(image);
            }
        }

        AlbumDTO album = AlbumDTO.builder()
                .id(albumNode.path("id").asText())
                .name(albumNode.path("name").asText())
                .uri(albumNode.path("uri").asText())
                .releaseDate(albumNode.path("release_date").asText())
                .totalTracks(albumNode.path("total_tracks").asInt())
                .images(images)
                .build();

        // Build track
        return TrackDTO.builder()
                .id(trackNode.path("id").asText())
                .name(trackNode.path("name").asText())
                .uri(trackNode.path("uri").asText())
                .durationMs(trackNode.path("duration_ms").asInt())
                .explicit(trackNode.path("explicit").asBoolean())
                .popularity(trackNode.path("popularity").asInt())
                .previewUrl(trackNode.path("preview_url").isNull() ? null : trackNode.path("preview_url").asText(null))
                .artists(artists)
                .album(album)
                .build();
    }

    @Value("${spring.security.oauth2.client.registration.spotify.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.spotify.client-secret}")
    private String clientSecret;

    public SpotifyTokenResponse refreshAccessToken(String refreshToken)
    {
        try{
            log.info("Refreshing Spotify access token...");

            RestTemplate restTemplate = new RestTemplate();

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // Encode client credentials
            String credentials = clientId + ":" + clientSecret;
            String encodedCredentials = java.util.Base64.getEncoder()
                    .encodeToString(credentials.getBytes());
            headers.set("Authorization", "Basic " + encodedCredentials);

            //Set body
            org.springframework.util.MultiValueMap<String, String> body =
                    new org.springframework.util.LinkedMultiValueMap<>();
            body.add("grant_type", "refresh_token");
            body.add("refresh_token", refreshToken);

            HttpEntity<org.springframework.util.MultiValueMap<String, String>> entity =
                    new HttpEntity<>(body, headers);

            ResponseEntity<SpotifyTokenResponse> response = restTemplate.exchange(
                    "https://accounts.spotify.com/api/token",
                        HttpMethod.POST,
                    entity,
                    SpotifyTokenResponse.class
                    );

            log.info("Token refreshed successfully!");
            return response.getBody();
        }catch (Exception e) {
            log.error("Error refreshing token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to refresh token", e);
        }
    }

    public SearchResultDTO getRecommendations(RecommendationRequestDTO request, String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Build URL with all parameters
            StringBuilder url = new StringBuilder(spotifyApiBaseUrl + "/recommendations?");

            // Add seeds (at least one required)
            if (request.getSeedTracks() != null && !request.getSeedTracks().isEmpty()) {
                url.append("seed_tracks=").append(request.getSeedTracks()).append("&");
            }
            if (request.getSeedArtists() != null && !request.getSeedArtists().isEmpty()) {
                url.append("seed_artists=").append(request.getSeedArtists()).append("&");
            }
            if (request.getSeedGenres() != null && !request.getSeedGenres().isEmpty()) {
                url.append("seed_genres=").append(request.getSeedGenres()).append("&");
            }

            // Add audio features
            if (request.getTargetEnergy() != null) {
                url.append("target_energy=").append(request.getTargetEnergy()).append("&");
            }
            if (request.getTargetValence() != null) {
                url.append("target_valence=").append(request.getTargetValence()).append("&");
            }
            if (request.getTargetDanceability() != null) {
                url.append("target_danceability=").append(request.getTargetDanceability()).append("&");
            }
            if (request.getTargetAcousticness() != null) {
                url.append("target_acousticness=").append(request.getTargetAcousticness()).append("&");
            }
            if (request.getTargetTempo() != null) {
                url.append("target_tempo=").append(request.getTargetTempo()).append("&");
            }
            if (request.getTargetPopularity() != null) {
                url.append("target_popularity=").append(request.getTargetPopularity()).append("&");
            }

            // Add limit
            url.append("limit=").append(request.getLimit() != null ? request.getLimit() : 20);

            log.info("Getting recommendations from Spotify...");

            ResponseEntity<String> response = restTemplate.exchange(
                    url.toString(),
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            // Parse response
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode tracksNode = root.path("tracks");

            List<TrackDTO> tracks = new ArrayList<>();
            if (tracksNode.isArray()) {
                for (JsonNode trackNode : tracksNode) {
                    tracks.add(parseTrack(trackNode));
                }
            }

            return SearchResultDTO.builder()
                    .tracks(tracks)
                    .total(tracks.size())
                    .limit(request.getLimit() != null ? request.getLimit() : 20)
                    .offset(0)
                    .build();

        } catch (Exception e) {
            log.error("Error getting recommendations: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get recommendations", e);
        }
    }

    public List<PlaylistDTO> getUserPlaylists(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    spotifyApiBaseUrl + "/me/playlists?limit=20",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode items = root.path("items");

            List<PlaylistDTO> playlists = new ArrayList<>();

            if (items.isArray()) {
                for (JsonNode item : items) {
                    List<ImageDTO> images = new ArrayList<>();
                    JsonNode imagesNode = item.path("images");
                    if (imagesNode.isArray()) {
                        for (JsonNode imageNode : imagesNode) {
                            ImageDTO image = new ImageDTO();
                            image.setHeight(imageNode.path("height").asInt());
                            image.setWidth(imageNode.path("width").asInt());
                            image.setUrl(imageNode.path("url").asText());
                            images.add(image);
                        }
                    }

                    PlaylistDTO playlist = PlaylistDTO.builder()
                            .id(item.path("id").asText())
                            .name(item.path("name").asText())
                            .description(item.path("description").asText())
                            .uri(item.path("uri").asText())
                            .totalTracks(item.path("tracks").path("total").asInt())
                            .images(images)
                            .isPublic(item.path("public").asBoolean())
                            .ownerName(item.path("owner").path("display_name").asText())
                            .build();

                    playlists.add(playlist);
                }
            }

            return playlists;

        } catch (Exception e) {
            log.error("Error fetching playlists: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch playlists", e);
        }
    }

    public String createPlaylist(String userId, String name,
                                 String description, String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            // Use ObjectMapper to build JSON string manually
            String jsonBody = objectMapper.writeValueAsString(Map.of(
                    "name", name,
                    "description", description,
                    "public", false
            ));

            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            log.info("Creating playlist '{}' for user: {}", name, userId);

            ResponseEntity<String> response = restTemplate.exchange(
                    spotifyApiBaseUrl + "/users/" + userId + "/playlists",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            log.info("Create playlist response: {}", response.getBody());

            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("id").asText();

        } catch (Exception e) {
            log.error("Error creating playlist: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create playlist: " + e.getMessage(), e);
        }
    }

    public void addTracksToPlaylist(String playlistId,
                                    List<String> trackUris,
                                    String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            // Use ObjectMapper to build JSON string manually
            String jsonBody = objectMapper.writeValueAsString(Map.of("uris", trackUris));
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            log.info("Adding {} tracks to playlist: {}", trackUris.size(), playlistId);

            restTemplate.exchange(
                    spotifyApiBaseUrl + "/playlists/" + playlistId + "/tracks",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            log.info("Tracks added successfully to playlist: {}", playlistId);

        } catch (Exception e) {
            log.error("Error adding tracks: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to add tracks: " + e.getMessage(), e);
        }
    }
}