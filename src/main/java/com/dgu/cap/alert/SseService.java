package com.dgu.cap.alert;

import com.dgu.cap.anomaly.AnomalyType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class SseService {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter addEmitter() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        return emitter;
    }

    public void sendNewAlert(String podName, AnomalyType anomalyType) {
        send("NEW_ALERT", Map.of("podName", podName, "anomalyType", anomalyType.name()));
    }

    public void sendTicketUpdated(Long ticketId, String status) {
        send("TICKET_UPDATED", Map.of("ticketId", ticketId, "status", status));
    }

    public void sendPodStatus(String podName, String phase) {
        send("POD_STATUS", Map.of("podName", podName, "phase", phase));
    }

    private void send(String eventType, Object data) {
        List<SseEmitter> dead = new CopyOnWriteArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventType).data(data));
            } catch (Exception e) {
                dead.add(emitter);
            }
        }
        emitters.removeAll(dead);
    }
}
