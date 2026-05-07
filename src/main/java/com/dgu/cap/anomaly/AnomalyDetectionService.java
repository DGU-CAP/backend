package com.dgu.cap.anomaly;

import com.dgu.cap.ai.AiResult;
import com.dgu.cap.ai.AiService;
import com.dgu.cap.ai.PodData;
import com.dgu.cap.alert.AlertService;
import com.dgu.cap.alert.SseService;
import com.dgu.cap.kubernetes.KubernetesService;
import com.dgu.cap.kubernetes.PodInfo;
import com.dgu.cap.metric.PrometheusService;
import com.dgu.cap.ticket.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnomalyDetectionService {

    private final KubernetesService kubernetesService;
    private final PrometheusService prometheusService;
    private final TicketService ticketService;
    private final AiService aiService;
    private final AlertService alertService;
    private final SseService sseService;
    private final AnomalyThresholdProperties thresholds;

    @Scheduled(fixedDelay = 30000)
    public void detectAnomalies() {
        List<PodInfo> pods = kubernetesService.getAllPods();
        for (PodInfo pod : pods) {
            try {
                checkPod(pod);
            } catch (Exception e) {
                log.warn("Pod 이상 탐지 중 오류 - pod: {}, error: {}", pod.getPodName(), e.getMessage());
            }
        }
    }

    private void checkPod(PodInfo pod) {
        String podName = pod.getPodName();
        String namespace = pod.getNamespace();

        double cpu = prometheusService.getCpuUsage(podName, namespace);
        if (cpu > thresholds.getCpu()) {
            handleAnomaly(pod, AnomalyType.CPU_HIGH, cpu, thresholds.getCpu());
        }

        double memory = prometheusService.getMemoryUsage(podName, namespace);
        if (memory > thresholds.getMemory()) {
            handleAnomaly(pod, AnomalyType.MEMORY_HIGH, memory, thresholds.getMemory());
        }

        if (pod.getRestartCount() >= thresholds.getRestart()) {
            handleAnomaly(pod, AnomalyType.POD_RESTART, pod.getRestartCount(), thresholds.getRestart());
        }

        double errorRate = prometheusService.getErrorRate(podName, namespace);
        if (errorRate > thresholds.getErrorRate()) {
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
        if (ticketService.isDuplicate(pod.getPodName(), anomalyType)) {
            return;
        }

        log.info("이상 탐지 - pod: {}, type: {}, value: {}", pod.getPodName(), anomalyType, metricValue);

        PodData podData = PodData.builder()
                .podName(pod.getPodName())
                .namespace(pod.getNamespace())
                .nodeName(pod.getNodeName())
                .anomalyType(anomalyType.name())
                .metricValue(metricValue)
                .threshold(threshold)
                .restartCount(pod.getRestartCount())
                .recentEvents(kubernetesService.getPodEventMessages(pod.getPodName(), pod.getNamespace()))
                .build();

        AiResult aiResult = aiService.analyze(podData);

        ticketService.createTicket(pod, anomalyType, metricValue, threshold, aiResult);
        alertService.sendTicketAlert(pod, anomalyType, aiResult);
        sseService.sendNewAlert(pod.getPodName(), anomalyType);
    }
}
