package com.dgu.cap.ticket;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TicketMetricSnapshotRepository extends JpaRepository<TicketMetricSnapshot, Long> {

    Optional<TicketMetricSnapshot> findByTicketId(Long ticketId);
}
