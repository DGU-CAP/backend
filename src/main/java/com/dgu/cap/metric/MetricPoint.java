package com.dgu.cap.metric;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class MetricPoint {

    private LocalDateTime timestamp;
    private Double value;
}
