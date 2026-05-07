package com.dgu.cap.metric;

import org.springframework.stereotype.Service;

@Service
public class PrometheusService {

    public double getCpuUsage(String podName, String namespace) {
        return 0.0;
    }

    public double getMemoryUsage(String podName, String namespace) {
        return 0.0;
    }

    public double getErrorRate(String podName, String namespace) {
        return 0.0;
    }
}
