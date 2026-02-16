package com.mzika.service;

import com.mzika.model.dto.MoodRequestDTO;
import com.mzika.model.dto.SearchResultDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final SpotifyService spotifyService;

    // Mood to search query mapping
    private static final Map<String, String[]> MOOD_QUERIES = new HashMap<>();

    static {
        MOOD_QUERIES.put("happy", new String[]{
                "happy upbeat feel good",
                "summer vibes cheerful",
                "positive energy dance",
                "good mood playlist"
        });
        MOOD_QUERIES.put("sad", new String[]{
                "sad emotional heartbreak",
                "melancholy slow ballad",
                "rainy day feelings",
                "lonely emotional"
        });
        MOOD_QUERIES.put("energetic", new String[]{
                "workout motivation high energy",
                "pump up running",
                "intense power music",
                "hype training"
        });
        MOOD_QUERIES.put("calm", new String[]{
                "calm relaxing peaceful",
                "chill lofi study",
                "ambient meditation",
                "sleep relax soft"
        });
        MOOD_QUERIES.put("romantic", new String[]{
                "romantic love sweet",
                "love songs couple",
                "date night slow dance",
                "tender love ballad"
        });
        MOOD_QUERIES.put("angry", new String[]{
                "aggressive intense raw",
                "rage powerful loud",
                "hard rock metal intense",
                "dark powerful"
        });
        MOOD_QUERIES.put("focused", new String[]{
                "focus concentration deep work",
                "study music instrumental",
                "productivity flow state",
                "concentration music"
        });
    }

    public SearchResultDTO getRecommendationsByMood(MoodRequestDTO request, String accessToken) {
        String mood = request.getMood() != null ? request.getMood().toLowerCase() : "happy";
        String genre = request.getGenre();
        String era = request.getEra();
        Integer limit = request.getLimit() != null ? request.getLimit() : 20;

        // Build smart search query
        String query = buildQuery(mood, genre, era);

        log.info("Getting recommendations for mood: {}, genre: {}, era: {}", mood, genre, era);
        log.info("Built query: {}", query);

        return spotifyService.searchTracks(query, accessToken, limit, 0);
    }

    private String buildQuery(String mood, String genre, String era) {
        StringBuilder query = new StringBuilder();

        // Add mood keywords
        if (MOOD_QUERIES.containsKey(mood)) {
            String[] moodOptions = MOOD_QUERIES.get(mood);
            // Pick a random query variation for variety
            String moodQuery = moodOptions[new Random().nextInt(moodOptions.length)];
            query.append(moodQuery);
        } else {
            query.append(mood);
        }

        // Add genre if provided
        if (genre != null && !genre.isEmpty()) {
            query.append(" ").append(genre);
        }

        // Add era/year filter if provided
        if (era != null && !era.isEmpty()) {
            switch (era.toLowerCase()) {
                case "2000s" -> query.append(" year:2000-2009");
                case "2010s" -> query.append(" year:2010-2019");
                case "2020s" -> query.append(" year:2020-2024");
                case "classic" -> query.append(" year:1960-1999");
                default -> query.append(" ").append(era);
            }
        }

        return query.toString();
    }

    public SearchResultDTO getSimilarTracks(String trackName,
                                            String artistName,
                                            String accessToken,
                                            Integer limit) {
        // Build query based on artist and similar keywords
        String query = "artist:" + artistName + " " + trackName;
        log.info("Getting similar tracks for: {} by {}", trackName, artistName);
        return spotifyService.searchTracks(query, accessToken, limit != null ? limit : 20, 0);
    }
}