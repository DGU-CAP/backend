package com.dgu.cap.alert;

import com.dgu.cap.ai.AiResult;
import com.dgu.cap.anomaly.AnomalyType;
import com.dgu.cap.kubernetes.PodInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final JavaMailSender mailSender;

    @Value("${alert.email}")
    private String alertEmail;

    public void sendTicketAlert(PodInfo pod, AnomalyType anomalyType, AiResult aiResult) {
        try {
            String subject = "[CAP 알람] " + pod.getPodName() + " - " + anomalyType.name();
            String body = buildEmailBody(pod, anomalyType, aiResult);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(alertEmail);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("알람 이메일 발송 - to: {}, pod: {}, type: {}", alertEmail, pod.getPodName(), anomalyType);
        } catch (Exception e) {
            log.warn("알람 이메일 발송 실패 - pod: {}, error: {}", pod.getPodName(), e.getMessage());
        }
    }

    private String buildEmailBody(PodInfo pod, AnomalyType anomalyType, AiResult aiResult) {
        StringBuilder sb = new StringBuilder();
        sb.append("== Kubernetes 이상 감지 알람 ==\n\n");
        sb.append("Pod: ").append(pod.getPodName()).append("\n");
        sb.append("Namespace: ").append(pod.getNamespace()).append("\n");
        sb.append("Node: ").append(pod.getNodeName()).append("\n");
        sb.append("이상 유형: ").append(anomalyType.name()).append("\n");

        if (aiResult != null) {
            sb.append("\n== AI 분석 결과 ==\n");
            sb.append("심각도: ").append(aiResult.getSeverity()).append("\n");
            sb.append("분석: ").append(aiResult.getAiAnalysis()).append("\n");
            sb.append("권고 조치: ").append(aiResult.getRecommendation()).append("\n");
        }

        return sb.toString();
    }
}
