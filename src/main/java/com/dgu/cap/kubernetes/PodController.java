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
            @RequestParam(defaultValue = "default") String namespace) {
        return ResponseEntity.ok(kubernetesService.getPods(namespace));
    }

    @GetMapping("/pods/{podName}/events")
    public ResponseEntity<List<PodEvent>> getPodEvents(
            @PathVariable String podName,
            @RequestParam(defaultValue = "default") String namespace) {
        return ResponseEntity.ok(kubernetesService.getPodEvents(podName, namespace));
    }

    @GetMapping("/topology")
    public ResponseEntity<Map<String, Object>> getTopology(
            @RequestParam(defaultValue = "default") String namespace) {
        List<PodInfo> pods = kubernetesService.getPods(namespace);
        return ResponseEntity.ok(Map.of("pods", pods, "namespace", namespace));
    }
}
