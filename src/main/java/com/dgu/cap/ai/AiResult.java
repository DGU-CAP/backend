package com.dgu.cap.ai;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiResult {

    private String aiAnalysis;
    private String recommendation;
    private String similarCases;
    private String severity;
}
