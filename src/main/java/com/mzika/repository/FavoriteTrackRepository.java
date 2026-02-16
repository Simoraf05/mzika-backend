package com.mzika.repository;

import com.mzika.model.entity.FavoriteTrack;
import com.mzika.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteTrackRepository extends JpaRepository<FavoriteTrack, String> {
    List<FavoriteTrack> findByUserOrderBySavedAtDesc(User user);
    Optional<FavoriteTrack> findByUserAndTrackId(User user, String trackId);
    boolean existsByUserAndTrackId(User user, String trackId);
    void deleteByUserAndTrackId(User user, String trackId);
    long countByUser(User user);
}
