package com.dgu.cap.alert;

import com.dgu.cap.ai.AiResult;
import com.dgu.cap.anomaly.AnomalyType;
import com.dgu.cap.kubernetes.PodInfo;
import org.springframework.stereotype.Service;

@Service
public class AlertService {

    public void sendTicketAlert(PodInfo pod, AnomalyType anomalyType, AiResult aiResult) {
    }
}
