package com.dgu.cap.ticket;

import jakarta.persistence.*;

@Entity
@Table(name = "incident_ticket")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
