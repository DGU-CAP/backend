package com.dgu.cap.ticket;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketActionLogRepository extends JpaRepository<TicketActionLog, Long> {

    List<TicketActionLog> findByTicketIdOrderByCreatedAtDesc(Long ticketId);
}
