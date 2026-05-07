package com.dgu.cap.log;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LokiService {

    private final RestTemplate restTemplate;

    @Value("${loki.url}")
    private String lokiUrl;

    private static final int DEFAULT_LIMIT = 20;
    private static final int RANGE_SECONDS = 300; // 최근 5분

    public List<String> getErrorLogs(String podName) {
        return getErrorLogs(podName, DEFAULT_LIMIT);
    }

    @SuppressWarnings("unchecked")
    public List<String> getErrorLogs(String podName, int limit) {
        try {
            long now = Instant.now().toEpochMilli() * 1_000_000L; // nanoseconds
            long start = Instant.now().minusSeconds(RANGE_SECONDS).toEpochMilli() * 1_000_000L;

            String query = String.format("{pod=\"%s\"} |= \"ERROR\"", podName);

            String url = UriComponentsBuilder.fromHttpUrl(lokiUrl + "/loki/api/v1/query_range")
                    .queryParam("query", query)
                    .queryParam("start", start)
                    .queryParam("end", now)
                    .queryParam("limit", limit)
                    .queryParam("direction", "backward")
                    .toUriString();

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) return Collections.emptyList();

            Map<String, Object> data = (Map<String, Object>) response.get("data");
            List<Map<String, Object>> result = (List<Map<String, Object>>) data.get("result");

            if (result == null || result.isEmpty()) return Collections.emptyList();

            return result.stream()
                    .flatMap(stream -> {
                        List<List<Object>> values = (List<List<Object>>) stream.get("values");
                        return values.stream().map(v -> (String) v.get(1));
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("Loki 로그 조회 실패 - pod: {}, error: {}", podName, e.getMessage());
            return Collections.emptyList();
        }
    }
}
