package com.dgu.cap.kubernetes;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PodController {

    private final KubernetesService kubernetesService;

    @GetMapping("/pods")
    public ResponseEntity<List<PodInfo>> getPods(
            @RequestParam(required = false) String namespace) {
        if (namespace == null || namespace.isBlank()) {
            return ResponseEntity.ok(kubernetesService.getAllPods());
        }
        return ResponseEntity.ok(kubernetesService.getPods(namespace));
    }

    @GetMapping("/pods/{podName}/events")
    public ResponseEntity<List<PodEvent>> getPodEvents(
            @PathVariable String podName,
            @RequestParam(required = false) String namespace) {
        String ns = (namespace == null || namespace.isBlank()) ? "default" : namespace;
        return ResponseEntity.ok(kubernetesService.getPodEvents(podName, ns));
    }

    @GetMapping("/topology")
    public ResponseEntity<Map<String, Object>> getTopology(
            @RequestParam(required = false) String namespace) {
        if (namespace == null || namespace.isBlank()) {
            List<PodInfo> pods = kubernetesService.getAllPods();
            return ResponseEntity.ok(Map.of("pods", pods, "namespace", "all"));
        }
        List<PodInfo> pods = kubernetesService.getPods(namespace);
        return ResponseEntity.ok(Map.of("pods", pods, "namespace", namespace));
    }
}
