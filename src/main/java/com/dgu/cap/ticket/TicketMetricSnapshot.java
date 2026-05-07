package com.dgu.cap.ticket;

import jakarta.persistence.*;

@Entity
@Table(name = "ticket_metric_snapshot")
public class TicketMetricSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
