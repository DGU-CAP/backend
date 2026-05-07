package com.dgu.cap.ticket;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByStatusOrderByCreatedAtDesc(String status);

    List<Ticket> findBySeverityOrderByCreatedAtDesc(String severity);

    List<Ticket> findByStatusAndSeverityOrderByCreatedAtDesc(String status, String severity);

    List<Ticket> findAllByOrderByCreatedAtDesc();
}
