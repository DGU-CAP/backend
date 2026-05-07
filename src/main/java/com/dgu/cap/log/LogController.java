package com.dgu.cap.log;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final LokiService lokiService;

    @GetMapping
    public ResponseEntity<List<String>> getErrorLogs(@RequestParam String pod) {
        return ResponseEntity.ok(lokiService.getErrorLogs(pod));
    }
}
