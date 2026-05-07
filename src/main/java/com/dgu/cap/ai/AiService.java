package com.dgu.cap.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final RestTemplate restTemplate;

    @Value("${ai.url}")
    private String aiUrl;

    public AiResult analyze(PodData podData) {
        try {
            AiResult result = restTemplate.postForObject(aiUrl + "/analyze", podData, AiResult.class);
            if (result == null) {
                return fallback(podData.getAnomalyType());
            }
            return result;
        } catch (Exception e) {
            log.warn("AI 분석 실패 (fallback 사용) - pod: {}, error: {}", podData.getPodName(), e.getMessage());
            return fallback(podData.getAnomalyType());
        }
    }

    private AiResult fallback(String anomalyType) {
        return AiResult.builder()
                .severity("MEDIUM")
                .aiAnalysis("AI 서버 응답 없음 - " + anomalyType + " 이상 감지됨")
                .recommendation("수동 확인 필요")
                .similarCases("")
                .build();
    }
}
