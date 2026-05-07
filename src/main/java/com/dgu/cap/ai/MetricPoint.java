package com.dgu.cap.ai;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MetricPoint {

    private long timestamp;
    private double value;
}
