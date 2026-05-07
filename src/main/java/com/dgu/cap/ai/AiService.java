package com.dgu.cap.ai;

import org.springframework.stereotype.Service;

@Service
public class AiService {

    public AiResult analyze(PodData podData) {
        return AiResult.builder().build();
    }
}
