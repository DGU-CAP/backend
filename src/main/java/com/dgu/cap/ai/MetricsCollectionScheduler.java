package com.dgu.cap.ai;

import com.dgu.cap.kubernetes.KubernetesService;
import com.dgu.cap.kubernetes.PodInfo;
import com.dgu.cap.log.LokiService;
import com.dgu.cap.metric.MetricPoint;
import com.dgu.cap.metric.PrometheusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsCollectionScheduler {

    private final KubernetesService kubernetesService;
    private final PrometheusService prometheusService;
    private final LokiService lokiService;
    private final AiService aiService;

    @Scheduled(fixedDelay = 60000)
    public void collectAndSendMetrics() {
        List<PodInfo> pods = kubernetesService.getAllPods();
        for (PodInfo pod : pods) {
            try {
                sendMetrics(pod);
            } catch (Exception e) {
                log.warn("메트릭 수집/전송 실패 - pod: {}, error: {}", pod.getPodName(), e.getMessage());
            }
        }
    }

    private void sendMetrics(PodInfo pod) {
        String podName = pod.getPodName();
        String namespace = pod.getNamespace();

        List<Double> cpuValues = toDoubleList(prometheusService.getCpuHistory(podName));
        List<Double> memoryValues = toDoubleList(prometheusService.getMemoryHistory(podName));
        List<Double> errorRateValues = toDoubleList(prometheusService.getErrorRateHistory(podName));

        MetricsData metrics = MetricsData.builder()
                .cpu(cpuValues.isEmpty() ? List.of(0.0) : cpuValues)
                .memory(memoryValues.isEmpty() ? List.of(0.0) : memoryValues)
                .errorRate(errorRateValues.isEmpty() ? List.of(0.0) : errorRateValues)
                .build();

        MetricsSendRequest request = MetricsSendRequest.builder()
                .podName(podName)
                .namespace(namespace)
                .nodeName(pod.getNodeName())
                .metrics(metrics)
                .restarts(pod.getRestartCount())
                .errorLogs(lokiService.getErrorLogs(podName))
                .k8sEvents(kubernetesService.getPodEventMessages(podName, namespace))
                .collectedAt(LocalDateTime.now())
                .build();

        aiService.sendMetrics(request);
    }

    private List<Double> toDoubleList(List<MetricPoint> points) {
        return points.stream()
                .map(MetricPoint::getValue)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
