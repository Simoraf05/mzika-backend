package com.mzika.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpotifyUserInfo {
    private String id;
    private String displayName;
    private String email;
    private String country;
    private String imageUrl;
    private String product;


}
