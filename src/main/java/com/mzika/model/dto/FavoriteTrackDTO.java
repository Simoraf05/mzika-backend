package com.mzika.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FavoriteTrackDTO {
    private String id;
    private String trackId;
    private String trackName;
    private String artistName;
    private String albumName;
    private String albumImageUrl;
    private Integer durationMs;
    private Integer popularity;
    private Boolean explicit;
    private LocalDateTime savedAt;
}
