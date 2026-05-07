package com.dgu.cap.ticket;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @GetMapping
    public ResponseEntity<List<Ticket>> getTickets(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity) {
        return ResponseEntity.ok(ticketService.getTickets(status, severity));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getTicket(@PathVariable Long id) {
        Ticket ticket = ticketService.getTicket(id);
        TicketMetricSnapshot snapshot = ticketService.getMetricSnapshot(id);
        List<TicketActionLog> actionLogs = ticketService.getActionLogs(id);

        return ResponseEntity.ok(Map.of(
                "ticket", ticket,
                "metricSnapshot", snapshot != null ? snapshot : Map.of(),
                "actionLogs", actionLogs
        ));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Ticket> updateStatus(
            @PathVariable Long id,
            @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(ticketService.updateStatus(id, request));
    }

    @GetMapping("/{id}/logs")
    public ResponseEntity<List<TicketActionLog>> getActionLogs(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getActionLogs(id));
    }
}
