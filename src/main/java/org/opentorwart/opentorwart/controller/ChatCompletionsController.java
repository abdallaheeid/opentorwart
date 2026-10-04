package org.opentorwart.opentorwart.controller;


import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/v1")
public class ChatCompletionsController {

    private final RestClient upstreamClient;
    private final JsonMapper jsonMapper;

    public ChatCompletionsController(RestClient upstreamClient, JsonMapper jsonMapper) {
        this.upstreamClient = upstreamClient;
        this.jsonMapper = jsonMapper;
    }

    @PostMapping(path = "/chat/completions", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StreamingResponseBody> chatCompletions(@RequestBody String requestBody) {
        boolean stream = jsonMapper.readTree(requestBody).path("stream").asBoolean(false);
        return stream ? streamCompletion(requestBody) : blockingCompletion(requestBody);
    }

    private ResponseEntity<StreamingResponseBody> blockingCompletion(String requestBody) {
        String body = upstreamClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(out -> out.write(body.getBytes(StandardCharsets.UTF_8)));
    }

    private ResponseEntity<StreamingResponseBody> streamCompletion(String requestBody) {
        StreamingResponseBody body = out -> upstreamClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .body(requestBody)
                .exchange((req, res) -> {
                    try (InputStream in = res.getBody()) {
                        byte[] buffer = new byte[1024];
                        int n;
                        while ((n = in.read(buffer)) != -1) {
                            out.write(buffer, 0, n);
                            out.flush();      // push each piece to the client now
                        }
                    }
                    return null;
                });

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(body);
    }

}
