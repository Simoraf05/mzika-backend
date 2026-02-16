package com.mzika.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationRequestDTO {

    // Seed data (at least one required)
    private String seedTracks;    // Comma-separated track IDs
    private String seedArtists;   // Comma-separated artist IDs
    private String seedGenres;    // Comma-separated genres

    // Audio features (0.0 to 1.0)
    private Float targetEnergy;      // 0=calm, 1=energetic
    private Float targetValence;     // 0=sad, 1=happy
    private Float targetDanceability; // 0=not danceable, 1=very danceable
    private Float targetAcousticness; // 0=electric, 1=acoustic

    // Other features
    private Integer targetTempo;     // BPM
    private Integer targetPopularity; // 0-100

    // Limit
    private Integer limit;
}