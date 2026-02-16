package com.mzika.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistDTO {
    private String id;
    private String name;
    private String description;
    private String uri;
    private Integer totalTracks;
    private List<ImageDTO> images;
    private Boolean isPublic;
    private String ownerName;
}