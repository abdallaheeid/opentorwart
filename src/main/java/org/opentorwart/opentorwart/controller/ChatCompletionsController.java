package org.opentorwart.opentorwart.controller;


import jakarta.servlet.http.HttpServletRequest;
import org.opentorwart.opentorwart.budget.BudgetService;
import org.opentorwart.opentorwart.budget.TokenEstimator;
import org.opentorwart.opentorwart.config.GatewayRequest;
import org.opentorwart.opentorwart.config.Reservation;
import org.opentorwart.opentorwart.filter.ApiKeyAuthFilter;
import org.opentorwart.opentorwart.logging.RequestLog;
import org.opentorwart.opentorwart.logging.RequestLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/v1")
public class ChatCompletionsController {

    private static final Logger log = LoggerFactory.getLogger(ChatCompletionsController.class);

    private final RestClient upstreamClient;
    private final JsonMapper jsonMapper;
    private final RequestLogService requestLogService;
    private final BudgetService budgetService;
    private final TokenEstimator tokenEstimator;

    public ChatCompletionsController(RestClient upstreamClient, JsonMapper jsonMapper, RequestLogService requestLogService, BudgetService budgetService, TokenEstimator tokenEstimator) {
        this.upstreamClient = upstreamClient;
        this.jsonMapper = jsonMapper;
        this.requestLogService = requestLogService;
        this.budgetService = budgetService;
        this.tokenEstimator = tokenEstimator;
    }

    @PostMapping(path = "/chat/completions", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StreamingResponseBody> chatCompletions(@RequestBody String requestBody,
                                                                 @RequestAttribute(ApiKeyAuthFilter.TEAM_ATTRIBUTE) String team,
                                                                 HttpServletRequest httpRequest) {
        log.info("Request from team= {}", team);
        JsonNode request = jsonMapper.readTree(requestBody);
        boolean stream = request.path("stream").asBoolean(false);
        String model = request.path("model").asString();

        GatewayRequest gr = new GatewayRequest(team, model, stream, System.nanoTime(), null);
        httpRequest.setAttribute(GatewayRequest.ATTRIBUTE, gr);

        Reservation reservation = budgetService.reserve(team, tokenEstimator.estimate(request));
        gr = gr.withReservation(reservation);
        httpRequest.setAttribute(GatewayRequest.ATTRIBUTE, gr);

        if (stream) {
            ObjectNode streamOptions = ((ObjectNode) request).putObject("stream_options");
            streamOptions.put("include_usage", true);
            requestBody = jsonMapper.writeValueAsString(request);
        }
        return stream ? streamCompletion(requestBody, gr) : blockingCompletion(requestBody, gr);
    }

    private ResponseEntity<StreamingResponseBody> blockingCompletion(String requestBody, GatewayRequest gr) {
        long start = System.nanoTime();
        String body = upstreamClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);
        long latencyMs = (System.nanoTime() - start) / 1_000_000;

        JsonNode usage = extractUsage(body);       // the safe helper: returns null on any problem
        budgetService.settle(gr.reservation(), actualTokens(usage, gr.reservation()));
        requestLogService.record(new RequestLog(gr.team(), gr.model(), false, 200, latencyMs,
                tokens(usage, "prompt_tokens"), tokens(usage, "completion_tokens")));

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(out -> out.write(body.getBytes(StandardCharsets.UTF_8)));
    }

    private ResponseEntity<StreamingResponseBody> streamCompletion(String requestBody, GatewayRequest gr) {
        long start = System.nanoTime();

        ClientHttpResponse upstream = upstreamClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .body(requestBody)
                .exchange((req, res) -> res, false);

        try {
            HttpStatusCode status = upstream.getStatusCode();
            if (status.isError()) {
                byte[] error = upstream.getBody().readAllBytes();
                upstream.close();
                budgetService.settle(gr.reservation(), 0);                  // full refund
                requestLogService.record(new RequestLog(gr.team(), gr.model(), true, status.value(),
                        (System.nanoTime() - start) / 1_000_000, null, null));
                return ResponseEntity.status(status)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(out -> out.write(error));
            }
        } catch (IOException e) {
            upstream.close();
            throw new ResourceAccessException("Failed to read upstream response", e);
        }

        StreamingResponseBody body = out -> {
            JsonNode usage = null;
            try (upstream;
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(upstream.getBody(), StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    out.write((line + "\n").getBytes(StandardCharsets.UTF_8));
                    if (line.isEmpty()) {
                        out.flush();                       // blank line = end of one SSE event
                    }
                    if (line.startsWith("data: ") && line.contains("\"usage\"")) {
                        usage = extractUsage(line.substring(6));
                    }
                }
            } finally {
                long latencyMs = (System.nanoTime() - start) / 1_000_000;
                budgetService.settle(gr.reservation(), actualTokens(usage, gr.reservation()));
                requestLogService.record(new RequestLog(gr.team(), gr.model(), true, 200, latencyMs,
                        tokens(usage, "prompt_tokens"), tokens(usage, "completion_tokens")));
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(body);
    }

    private JsonNode extractUsage(String json) {
        try {
            JsonNode usage = jsonMapper.readTree(json).path("usage");
            return usage.isObject() ? usage : null;
        } catch (Exception e) {
            return null;          // never break the stream because of logging
        }
    }

    private long actualTokens(JsonNode usage, Reservation reservation) {
        if (usage == null) {
            return reservation.reservedTokens();   // actual unknown → keep the estimate
        }
        return usage.path("prompt_tokens").asLong() + usage.path("completion_tokens").asLong();
    }

    private static Integer tokens(JsonNode usage, String field) {
        return usage == null ? null : usage.path(field).asInt();
    }

}
