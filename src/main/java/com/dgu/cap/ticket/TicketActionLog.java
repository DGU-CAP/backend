package com.dgu.cap.ticket;

import jakarta.persistence.*;

@Entity
@Table(name = "ticket_action_log")
public class TicketActionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
