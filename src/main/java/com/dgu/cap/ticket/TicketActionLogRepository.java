package com.dgu.cap.ticket;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketActionLogRepository extends JpaRepository<TicketActionLog, Long> {
}
