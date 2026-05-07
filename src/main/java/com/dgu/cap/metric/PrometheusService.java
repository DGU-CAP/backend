package com.dgu.cap.metric;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrometheusService {

    private final RestTemplate restTemplate;

    @Value("${prometheus.url}")
    private String prometheusUrl;

    // PromQL 쿼리 상수
    private static final String QUERY_CPU =
            "rate(container_cpu_usage_seconds_total{pod=\"%s\",container!=\"\"}[5m]) * 100";
    private static final String QUERY_MEMORY =
            "container_memory_usage_bytes{pod=\"%s\",container!=\"\"} / container_spec_memory_limit_bytes{pod=\"%s\",container!=\"\",container_spec_memory_limit_bytes!=0} * 100";
    private static final String QUERY_RESTARTS =
            "kube_pod_container_status_restarts_total{pod=\"%s\"}";
    private static final String QUERY_ERROR_RATE =
            "rate(http_requests_total{pod=\"%s\",status=~\"5..\"}[5m]) / rate(http_requests_total{pod=\"%s\"}[5m]) * 100";

    public CurrentMetric getCurrentMetrics(String podName) {
        return CurrentMetric.builder()
                .podName(podName)
                .cpu(querySingleValue(String.format(QUERY_CPU, podName)))
                .memory(querySingleValue(String.format(QUERY_MEMORY, podName, podName)))
                .restarts(queryRestarts(podName))
                .errorRate(querySingleValue(String.format(QUERY_ERROR_RATE, podName, podName)))
                .build();
    }

    public List<MetricPoint> getRangeMetrics(String podName, String metricType, int rangeSeconds) {
        String query = buildRangeQuery(podName, metricType);
        if (query == null) return Collections.emptyList();
        return queryRange(query, rangeSeconds);
    }

    // AnomalyDetectionService에서 호출하는 단순 값 조회 메서드
    public Double getCpu(String podName) {
        return querySingleValue(String.format(QUERY_CPU, podName));
    }

    public Double getMemory(String podName) {
        return querySingleValue(String.format(QUERY_MEMORY, podName, podName));
    }

    public Integer getRestarts(String podName) {
        return queryRestarts(podName);
    }

    public Double getErrorRate(String podName) {
        return querySingleValue(String.format(QUERY_ERROR_RATE, podName, podName));
    }

    public List<MetricPoint> getCpuHistory(String podName) {
        return queryRange(String.format(QUERY_CPU, podName), 1800);
    }

    public List<MetricPoint> getMemoryHistory(String podName) {
        return queryRange(String.format(QUERY_MEMORY, podName, podName), 1800);
    }

    public List<MetricPoint> getErrorRateHistory(String podName) {
        return queryRange(String.format(QUERY_ERROR_RATE, podName, podName), 1800);
    }

    @SuppressWarnings("unchecked")
    private Double querySingleValue(String query) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(prometheusUrl + "/api/v1/query")
                    .queryParam("query", query)
                    .toUriString();

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) return null;

            Map<String, Object> data = (Map<String, Object>) response.get("data");
            List<Map<String, Object>> result = (List<Map<String, Object>>) data.get("result");

            if (result == null || result.isEmpty()) return null;

            List<Object> value = (List<Object>) result.get(0).get("value");
            return Double.parseDouble((String) value.get(1));
        } catch (Exception e) {
            log.warn("Prometheus 단일값 조회 실패 - query: {}, error: {}", query, e.getMessage());
            return null;
        }
    }

    private Integer queryRestarts(String podName) {
        Double value = querySingleValue(String.format(QUERY_RESTARTS, podName));
        return value != null ? value.intValue() : null;
    }

    @SuppressWarnings("unchecked")
    private List<MetricPoint> queryRange(String query, int rangeSeconds) {
        try {
            long now = Instant.now().getEpochSecond();
            long start = now - rangeSeconds;

            String url = UriComponentsBuilder.fromHttpUrl(prometheusUrl + "/api/v1/query_range")
                    .queryParam("query", query)
                    .queryParam("start", start)
                    .queryParam("end", now)
                    .queryParam("step", 300)
                    .toUriString();

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) return Collections.emptyList();

            Map<String, Object> data = (Map<String, Object>) response.get("data");
            List<Map<String, Object>> result = (List<Map<String, Object>>) data.get("result");

            if (result == null || result.isEmpty()) return Collections.emptyList();

            List<List<Object>> values = (List<List<Object>>) result.get(0).get("values");
            return values.stream()
                    .map(v -> {
                        long ts = ((Number) v.get(0)).longValue();
                        double val = Double.parseDouble((String) v.get(1));
                        LocalDateTime time = LocalDateTime.ofInstant(
                                Instant.ofEpochSecond(ts), ZoneId.systemDefault());
                        return new MetricPoint(time, val);
                    })
                    .toList();
        } catch (Exception e) {
            log.warn("Prometheus 시계열 조회 실패 - query: {}, error: {}", query, e.getMessage());
            return Collections.emptyList();
        }
    }

    private String buildRangeQuery(String podName, String metricType) {
        return switch (metricType) {
            case "cpu" -> String.format(QUERY_CPU, podName);
            case "memory" -> String.format(QUERY_MEMORY, podName, podName);
            case "error-rate" -> String.format(QUERY_ERROR_RATE, podName, podName);
            default -> {
                log.warn("알 수 없는 메트릭 타입: {}", metricType);
                yield null;
            }
        };
    }
}
