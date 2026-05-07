package com.dgu.cap.ai;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AiResult {

    private String severity;
    private String aiAnalysis;
    private String recommendation;
    private List<String> similarCases;
}
