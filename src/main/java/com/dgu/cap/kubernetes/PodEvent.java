package com.dgu.cap.kubernetes;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PodEvent {

    private String type;
    private String reason;
    private String message;
    private int count;
}
