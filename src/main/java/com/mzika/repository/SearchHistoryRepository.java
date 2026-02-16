package com.mzika.repository;

import com.mzika.model.entity.SearchHistory;
import com.mzika.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface  SearchHistoryRepository extends JpaRepository<SearchHistory, String> {

    @Query("SELECT s FROM SearchHistory s WHERE s.user = ?1 ORDER BY s.searchedAt DESC LIMIT 10")
    List<SearchHistory> findTop10ByUserOrderBySearchedAtDesc(User user);

    void deleteByUser(User user);

    long countByUser(User user);

    @Query("SELECT s.query, COUNT(s) as count FROM SearchHistory s WHERE s.user = ?1 GROUP BY s.query ORDER BY count DESC LIMIT 5")
    List<Object[]> findTopQueriesByUser(User user);
}
