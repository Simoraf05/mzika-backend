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
public class AlbumDTO {
    private String id;
    private String name;
    private String uri;
    private String releaseDate;
    private Integer totalTracks;
    private List<ImageDTO> images;
}
