package com.mzika.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TrackDTO {
    private String id;
    private String name;
    private String uri;
    private Integer durationMs;
    private Boolean explicit;
    private Integer popularity;
    private String previewUrl;
    private List<ArtistDTO> artists;
    private AlbumDTO album;
}
