package com.dgu.cap.ai;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MetricsData {

    private List<Double> cpu;
    private List<Double> memory;
    private List<Double> errorRate;
}
