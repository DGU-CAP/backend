package com.dgu.cap.metric;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CurrentMetric {

    private String podName;
    private Double cpu;
    private Double memory;
    private Integer restarts;
    private Double errorRate;
}
