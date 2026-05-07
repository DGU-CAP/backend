package com.dgu.cap.metric;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class MetricController {

    private final PrometheusService prometheusService;

    @GetMapping("/current")
    public ResponseEntity<CurrentMetric> getCurrentMetrics(@RequestParam String pod) {
        return ResponseEntity.ok(prometheusService.getCurrentMetrics(pod));
    }

    // range: 1h, 6h, 24h 등 — 초 단위로 변환
    @GetMapping("/range")
    public ResponseEntity<List<MetricPoint>> getRangeMetrics(
            @RequestParam String pod,
            @RequestParam String metric,
            @RequestParam(defaultValue = "1h") String range) {
        int rangeSeconds = parseRange(range);
        return ResponseEntity.ok(prometheusService.getRangeMetrics(pod, metric, rangeSeconds));
    }

    private int parseRange(String range) {
        return switch (range) {
            case "30m" -> 1800;
            case "1h" -> 3600;
            case "6h" -> 21600;
            case "24h" -> 86400;
            default -> 3600;
        };
    }
}
