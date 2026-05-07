package com.dgu.cap.ticket;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "incident_ticket")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ticketNumber;
    private String title;
    private String podName;
    private String namespace;
    private String nodeName;
    private String anomalyType;

    @Column(precision = 10, scale = 2)
    private BigDecimal metricValue;

    @Column(precision = 10, scale = 2)
    private BigDecimal threshold;

    private String severity;
    private String status;

    @Column(columnDefinition = "TEXT")
    private String aiAnalysis;

    @Column(columnDefinition = "TEXT")
    private String recommendation;

    @Column(columnDefinition = "TEXT")
    private String similarCases;

    private String assigneeName;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime resolvedAt;

    public void updateStatus(String newStatus) {
        this.status = newStatus;
        if ("RESOLVED".equals(newStatus) || "CLOSED".equals(newStatus)) {
            this.resolvedAt = LocalDateTime.now();
        }
    }

    public void initTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }
}
