package com.dgu.cap.ai;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PodData {

    private String podName;
    private String namespace;
    private String nodeName;
    private String anomalyType;
    private double metricValue;
    private double threshold;
    private int restartCount;
    private List<String> recentEvents;
    private List<MetricPoint> cpuHistory;
    private List<MetricPoint> memoryHistory;
}
