package com.mzika.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "favorite_tracks",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "track_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteTrack {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "track_id", nullable = false)
    private String trackId;

    @Column(nullable = false)
    private String trackName;

    @Column(nullable = false)
    private String artistName;

    private String albumName;
    private String albumImageUrl;
    private Integer durationMs;
    private Integer popularity;
    private Boolean explicit;

    private LocalDateTime savedAt;

    @PrePersist
    protected void onCreate() {
        savedAt = LocalDateTime.now();
    }
}