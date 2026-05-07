package com.dgu.cap.kubernetes;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PodInfo {

    private String podName;
    private String namespace;
    private String nodeName;
    private String phase;
    private boolean ready;
    private int restartCount;
    private List<String> containers;
}
