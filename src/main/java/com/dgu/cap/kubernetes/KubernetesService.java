package com.dgu.cap.kubernetes;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.ClientBuilder;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KubernetesService {

    private CoreV1Api coreV1Api;

    @PostConstruct
    public void init() {
        try {
            // K8s Pod 내부 실행 시 (in-cluster config)
            ApiClient client = ClientBuilder.cluster().build();
            Configuration.setDefaultApiClient(client);
            log.info("Kubernetes in-cluster config 로드 완료");
        } catch (IOException e) {
            try {
                // 로컬 개발 환경 (kubeconfig)
                ApiClient client = ClientBuilder.standard().build();
                Configuration.setDefaultApiClient(client);
                log.info("Kubernetes kubeconfig 로드 완료");
            } catch (IOException ex) {
                log.warn("Kubernetes client 초기화 실패 - K8s 연동 비활성화: {}", ex.getMessage());
                return;
            }
        }
        this.coreV1Api = new CoreV1Api();
    }

    public List<PodInfo> getPods(String namespace) {
        if (coreV1Api == null) return Collections.emptyList();
        try {
            V1PodList podList = coreV1Api.listNamespacedPod(namespace)
                    .execute();
            return podList.getItems().stream()
                    .map(this::toPodInfo)
                    .collect(Collectors.toList());
        } catch (ApiException e) {
            log.warn("Pod 목록 조회 실패 - namespace: {}, error: {}", namespace, e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<PodInfo> getAllPods() {
        if (coreV1Api == null) return Collections.emptyList();
        try {
            V1PodList podList = coreV1Api.listPodForAllNamespaces().execute();
            return podList.getItems().stream()
                    .map(this::toPodInfo)
                    .collect(Collectors.toList());
        } catch (ApiException e) {
            log.warn("전체 Pod 목록 조회 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<PodEvent> getPodEvents(String podName, String namespace) {
        if (coreV1Api == null) return Collections.emptyList();
        try {
            String fieldSelector = "involvedObject.name=" + podName;
            V1EventList eventList = coreV1Api.listNamespacedEvent(namespace)
                    .fieldSelector(fieldSelector)
                    .execute();
            return eventList.getItems().stream()
                    .map(this::toPodEvent)
                    .collect(Collectors.toList());
        } catch (ApiException e) {
            log.warn("Pod 이벤트 조회 실패 - pod: {}, error: {}", podName, e.getMessage());
            return Collections.emptyList();
        }
    }

    public boolean hasOomKilled(String podName, String namespace) {
        return getPodEvents(podName, namespace).stream()
                .anyMatch(e -> "OOMKilling".equals(e.getReason()) || "OOMKilled".equals(e.getReason()));
    }

    public boolean hasCrashLoopBackOff(String podName, String namespace) {
        return getPodEvents(podName, namespace).stream()
                .anyMatch(e -> "BackOff".equals(e.getReason()));
    }

    public List<String> getPodEventMessages(String podName, String namespace) {
        return getPodEvents(podName, namespace).stream()
                .map(PodEvent::getMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private PodInfo toPodInfo(V1Pod pod) {
        V1PodSpec spec = pod.getSpec();
        V1PodStatus status = pod.getStatus();

        List<String> containers = spec != null && spec.getContainers() != null
                ? spec.getContainers().stream()
                        .map(V1Container::getName)
                        .collect(Collectors.toList())
                : Collections.emptyList();

        int restartCount = 0;
        boolean ready = false;
        if (status != null && status.getContainerStatuses() != null) {
            restartCount = status.getContainerStatuses().stream()
                    .mapToInt(cs -> cs.getRestartCount() != null ? cs.getRestartCount() : 0)
                    .sum();
            ready = status.getContainerStatuses().stream()
                    .allMatch(cs -> Boolean.TRUE.equals(cs.getReady()));
        }

        return PodInfo.builder()
                .podName(pod.getMetadata() != null ? pod.getMetadata().getName() : "unknown")
                .namespace(pod.getMetadata() != null ? pod.getMetadata().getNamespace() : "unknown")
                .nodeName(spec != null ? spec.getNodeName() : null)
                .phase(status != null ? status.getPhase() : "Unknown")
                .ready(ready)
                .restartCount(restartCount)
                .containers(containers)
                .build();
    }

    private PodEvent toPodEvent(V1Event event) {
        return PodEvent.builder()
                .type(event.getType())
                .reason(event.getReason())
                .message(event.getMessage())
                .count(event.getCount() != null ? event.getCount() : 0)
                .build();
    }
}
