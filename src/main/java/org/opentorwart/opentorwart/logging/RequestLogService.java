package org.opentorwart.opentorwart.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RequestLogService {

    private static final Logger log = LoggerFactory.getLogger(RequestLogService.class);

    public void record(RequestLog requestLog) {
        try {
            log.info("Request: {}", requestLog);  // later: DB / Kafka
        } catch (Exception e) {
            log.warn("Failed to record request log", e);
        }
    }

}