package com.mzika.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoodRequestDTO {
    private String mood;    // happy, sad, energetic, calm, romantic, angry
    private String genre;   // pop, rock, hip-hop, jazz, etc.
    private String era;     // 2000s, 2010s, 2020s, classic
    private Integer limit;
}
