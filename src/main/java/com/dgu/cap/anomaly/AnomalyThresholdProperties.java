package com.dgu.cap.anomaly;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "anomaly.threshold")
public class AnomalyThresholdProperties {

    private double cpu;
    private double memory;
    private int restart;
    private double errorRate;
}
