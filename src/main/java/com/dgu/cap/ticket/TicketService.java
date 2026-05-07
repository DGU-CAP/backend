package com.dgu.cap.ticket;

import com.dgu.cap.ai.AiResult;
import com.dgu.cap.anomaly.AnomalyType;
import com.dgu.cap.kubernetes.PodInfo;
import org.springframework.stereotype.Service;

@Service
public class TicketService {

    public boolean isDuplicate(String podName, AnomalyType anomalyType) {
        return false;
    }

    public Ticket createTicket(PodInfo pod, AnomalyType anomalyType, double metricValue, double threshold, AiResult aiResult) {
        return null;
    }
}
