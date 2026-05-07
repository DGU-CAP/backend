package com.dgu.cap.ticket;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CreateTicketRequest {

    private String podName;
    private String namespace;
    private String nodeName;
    private String anomalyType;
    private BigDecimal metricValue;
    private BigDecimal threshold;

    // AI 결과
    private String severity;
    private String aiAnalysis;
    private String recommendation;
    private String similarCases;

    // 메트릭 스냅샷
    private BigDecimal cpu;
    private BigDecimal memory;
    private Integer restarts;
    private BigDecimal errorRate;
}
