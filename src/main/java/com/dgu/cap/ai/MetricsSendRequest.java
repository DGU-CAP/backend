package com.dgu.cap.ai;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class MetricsSendRequest {

    private String podName;
    private String namespace;
    private String nodeName;
    private MetricsData metrics;
    private int restarts;
    private List<String> errorLogs;
    private List<String> k8sEvents;
    private LocalDateTime collectedAt;
}
