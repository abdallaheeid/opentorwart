package org.opentorwart.opentorwart.audit;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final AuditRepository auditRepository;

    public AuditService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Async
    public void record(AuditEvent event) {
        try {
            auditRepository.insert(event);
        } catch (Exception e) {
            log.error("Failed to record audit event requestId={}", event.requestId(), e);
        }
    }

}
