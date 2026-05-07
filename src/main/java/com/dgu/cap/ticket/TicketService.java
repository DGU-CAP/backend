package com.dgu.cap.ticket;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketActionLogRepository actionLogRepository;
    private final TicketMetricSnapshotRepository metricSnapshotRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String REDIS_KEY_PREFIX = "ticket:";
    private static final long DEDUP_TTL_MINUTES = 10;

    public boolean isDuplicate(String podName, String anomalyType) {
        String key = REDIS_KEY_PREFIX + podName + ":" + anomalyType;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Transactional
    public Ticket createTicket(CreateTicketRequest request) {
        String ticketNumber = generateTicketNumber();

        Ticket ticket = Ticket.builder()
                .ticketNumber(ticketNumber)
                .title(request.getPodName() + " " + request.getAnomalyType() + " 감지")
                .podName(request.getPodName())
                .namespace(request.getNamespace())
                .nodeName(request.getNodeName())
                .anomalyType(request.getAnomalyType())
                .metricValue(request.getMetricValue())
                .threshold(request.getThreshold())
                .severity(request.getSeverity())
                .status("OPEN")
                .aiAnalysis(request.getAiAnalysis())
                .recommendation(request.getRecommendation())
                .similarCases(request.getSimilarCases())
                .build();

        ticket = ticketRepository.save(ticket);

        if (request.getCpu() != null) {
            TicketMetricSnapshot snapshot = TicketMetricSnapshot.builder()
                    .ticket(ticket)
                    .cpu(request.getCpu())
                    .memory(request.getMemory())
                    .restarts(request.getRestarts())
                    .errorRate(request.getErrorRate())
                    .capturedAt(LocalDateTime.now())
                    .build();
            metricSnapshotRepository.save(snapshot);
        }

        String redisKey = REDIS_KEY_PREFIX + request.getPodName() + ":" + request.getAnomalyType();
        redisTemplate.opsForValue().set(redisKey, "1", DEDUP_TTL_MINUTES, TimeUnit.MINUTES);

        return ticket;
    }

    public List<Ticket> getTickets(String status, String severity) {
        if (status != null && severity != null) {
            return ticketRepository.findByStatusAndSeverityOrderByCreatedAtDesc(status, severity);
        }
        if (status != null) {
            return ticketRepository.findByStatusOrderByCreatedAtDesc(status);
        }
        if (severity != null) {
            return ticketRepository.findBySeverityOrderByCreatedAtDesc(severity);
        }
        return ticketRepository.findAllByOrderByCreatedAtDesc();
    }

    public Ticket getTicket(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("티켓을 찾을 수 없습니다. id=" + id));
    }

    public TicketMetricSnapshot getMetricSnapshot(Long ticketId) {
        return metricSnapshotRepository.findByTicketId(ticketId).orElse(null);
    }

    @Transactional
    public Ticket updateStatus(Long id, UpdateStatusRequest request) {
        Ticket ticket = getTicket(id);
        ticket.updateStatus(request.getStatus());

        if (request.getAction() != null) {
            TicketActionLog log = TicketActionLog.builder()
                    .ticket(ticket)
                    .action(request.getAction())
                    .memo(request.getMemo())
                    .performedBy(request.getPerformedBy())
                    .build();
            actionLogRepository.save(log);
        }

        return ticket;
    }

    public List<TicketActionLog> getActionLogs(Long ticketId) {
        return actionLogRepository.findByTicketIdOrderByCreatedAtDesc(ticketId);
    }

    private String generateTicketNumber() {
        int year = LocalDate.now().getYear();
        long count = ticketRepository.count() + 1;
        return String.format("TKT-%d-%03d", year, count);
    }
}
