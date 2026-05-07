package com.dgu.cap.ticket;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_metric_snapshot")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketMetricSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    @Column(precision = 10, scale = 2)
    private BigDecimal cpu;

    @Column(precision = 10, scale = 2)
    private BigDecimal memory;

    private Integer restarts;

    @Column(precision = 10, scale = 2)
    private BigDecimal errorRate;

    private LocalDateTime capturedAt;
}
