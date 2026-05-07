package com.dgu.cap.ticket;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_action_log")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketActionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    private String action;

    @Column(columnDefinition = "TEXT")
    private String memo;

    private String performedBy;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
