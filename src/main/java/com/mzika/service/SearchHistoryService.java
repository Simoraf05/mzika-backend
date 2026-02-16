package com.mzika.service;

import com.mzika.model.entity.SearchHistory;
import com.mzika.model.entity.User;
import com.mzika.repository.SearchHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchHistoryService {

    private final SearchHistoryRepository searchHistoryRepository;

    @Transactional
    public void saveSearch(User user, String query, int resultsCount) {
        SearchHistory history = SearchHistory.builder()
                .user(user)
                .query(query)
                .resultsCount(resultsCount)
                .build();
        searchHistoryRepository.save(history);
    }

    public List<SearchHistory> getRecentSearches(User user) {
        return searchHistoryRepository.findTop10ByUserOrderBySearchedAtDesc(user);
    }

    public Map<String, Object> getStats(User user) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalSearches", searchHistoryRepository.countByUser(user));

        List<Object[]> topQueries = searchHistoryRepository.findTopQueriesByUser(user);
        stats.put("topQueries", topQueries.stream()
                .map(row -> Map.of("query", row[0], "count", row[1]))
                .toList());

        return stats;
    }

    @Transactional
    public void clearHistory(User user) {
        searchHistoryRepository.deleteByUser(user);
    }
}