package com.dgu.cap.anomaly;

import com.dgu.cap.ai.AiResult;
import com.dgu.cap.ai.AiService;
import com.dgu.cap.ai.MetricsData;
import com.dgu.cap.ai.PodData;
import com.dgu.cap.alert.AlertService;
import com.dgu.cap.alert.SseService;
import com.dgu.cap.kubernetes.KubernetesService;
import com.dgu.cap.kubernetes.PodInfo;
import com.dgu.cap.log.LokiService;
import com.dgu.cap.metric.MetricPoint;
import com.dgu.cap.metric.PrometheusService;
import com.dgu.cap.ticket.CreateTicketRequest;
import com.dgu.cap.ticket.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnomalyDetectionService {

    private final KubernetesService kubernetesService;
    private final PrometheusService prometheusService;
    private final LokiService lokiService;
    private final TicketService ticketService;
    private final AiService aiService;
    private final AlertService alertService;
    private final SseService sseService;
    private final AnomalyThresholdProperties thresholds;

    @Scheduled(fixedDelay = 30000)
    public void detectAnomalies() {
        try {
            List<PodInfo> pods = kubernetesService.getAllPods();
            for (PodInfo pod : pods) {
                try {
                    checkPod(pod);
                } catch (Exception e) {
                    log.warn("Pod 이상 탐지 중 오류 - pod: {}, error: {}", pod.getPodName(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("이상 탐지 스케줄러 전체 실패: {}", e.getMessage());
        }
    }

    private void checkPod(PodInfo pod) {
        String podName = pod.getPodName();
        String namespace = pod.getNamespace();

        Double cpu = prometheusService.getCpu(podName);
        if (cpu != null && cpu > thresholds.getCpu()) {
            handleAnomaly(pod, AnomalyType.CPU_HIGH, cpu, thresholds.getCpu());
        }

        Double memory = prometheusService.getMemory(podName);
        if (memory != null && memory > thresholds.getMemory()) {
            handleAnomaly(pod, AnomalyType.MEMORY_HIGH, memory, thresholds.getMemory());
        }

        if (pod.getRestartCount() >= thresholds.getRestart()) {
            handleAnomaly(pod, AnomalyType.POD_RESTART, pod.getRestartCount(), thresholds.getRestart());
        }

        Double errorRate = prometheusService.getErrorRate(podName);
        if (errorRate != null && errorRate > thresholds.getErrorRate()) {
            handleAnomaly(pod, AnomalyType.ERROR_RATE_HIGH, errorRate, thresholds.getErrorRate());
        }

        if (kubernetesService.hasOomKilled(podName, namespace)) {
            handleAnomaly(pod, AnomalyType.OOM_KILLED, 0, 0);
        }

        if (kubernetesService.hasCrashLoopBackOff(podName, namespace)) {
            handleAnomaly(pod, AnomalyType.CRASH_LOOP, 0, 0);
        }
    }

    private void handleAnomaly(PodInfo pod, AnomalyType anomalyType, double metricValue, double threshold) {
        String podName = pod.getPodName();
        String namespace = pod.getNamespace();

        if (ticketService.isDuplicate(podName, anomalyType.name())) {
            return;
        }

        log.info("이상 탐지 - pod: {}, type: {}, value: {}", podName, anomalyType, metricValue);

        List<Double> cpuValues = toDoubleList(prometheusService.getCpuHistory(podName));
        List<Double> memoryValues = toDoubleList(prometheusService.getMemoryHistory(podName));
        List<Double> errorRateValues = toDoubleList(prometheusService.getErrorRateHistory(podName));

        MetricsData metrics = MetricsData.builder()
                .cpu(cpuValues.isEmpty() ? List.of(0.0) : cpuValues)
                .memory(memoryValues.isEmpty() ? List.of(0.0) : memoryValues)
                .errorRate(errorRateValues.isEmpty() ? List.of(0.0) : errorRateValues)
                .build();

        PodData podData = PodData.builder()
                .podName(podName)
                .namespace(namespace)
                .nodeName(pod.getNodeName())
                .anomalyType(anomalyType.name())
                .metrics(metrics)
                .restarts(pod.getRestartCount())
                .errorLogs(lokiService.getErrorLogs(podName))
                .k8sEvents(kubernetesService.getPodEventMessages(podName, namespace))
                .detectedAt(LocalDateTime.now())
                .build();

        AiResult aiResult = aiService.analyze(podData);

        Double currentCpu = prometheusService.getCpu(podName);
        Double currentMemory = prometheusService.getMemory(podName);
        Integer restarts = prometheusService.getRestarts(podName);
        Double currentErrorRate = prometheusService.getErrorRate(podName);

        String similarCasesStr = aiResult.getSimilarCases() != null
                ? String.join("\n", aiResult.getSimilarCases())
                : null;

        CreateTicketRequest request = CreateTicketRequest.builder()
                .podName(pod.getPodName())
                .namespace(pod.getNamespace())
                .nodeName(pod.getNodeName())
                .anomalyType(anomalyType.name())
                .metricValue(metricValue > 0 ? BigDecimal.valueOf(metricValue) : null)
                .threshold(threshold > 0 ? BigDecimal.valueOf(threshold) : null)
                .severity(aiResult.getSeverity())
                .aiAnalysis(aiResult.getAiAnalysis())
                .recommendation(aiResult.getRecommendation())
                .similarCases(similarCasesStr)
                .cpu(currentCpu != null ? BigDecimal.valueOf(currentCpu) : null)
                .memory(currentMemory != null ? BigDecimal.valueOf(currentMemory) : null)
                .restarts(restarts)
                .errorRate(currentErrorRate != null ? BigDecimal.valueOf(currentErrorRate) : null)
                .build();

        ticketService.createTicket(request);
        alertService.sendTicketAlert(pod, anomalyType, aiResult);
        sseService.sendNewAlert(podName, anomalyType);
    }

    private List<Double> toDoubleList(List<MetricPoint> points) {
        return points.stream()
                .map(MetricPoint::getValue)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
