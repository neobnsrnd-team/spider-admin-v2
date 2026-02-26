package org.example.springadminv2.global.log.adapter;

import java.time.YearMonth;

import org.elasticsearch.client.Request;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;
import org.example.springadminv2.global.log.adapter.document.LogEventDocument;
import org.example.springadminv2.global.log.event.LogEvent;
import org.example.springadminv2.global.log.port.LogEventPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "log.event.elasticsearch.enabled", havingValue = "true")
public class ElasticsearchLogEventAdapter implements LogEventPort {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Override
    public void record(LogEvent event) {
        try {
            LogEventDocument doc = LogEventDocument.from(event);
            String index = resolveIndex(event);
            String source = objectMapper.writeValueAsString(doc);

            Request request = new Request("POST", "/" + index + "/_doc");
            request.setJsonEntity(source);

            restClient.performRequestAsync(request, new ResponseListener() {
                @Override
                public void onSuccess(org.elasticsearch.client.Response response) {
                    // indexed successfully
                }

                @Override
                public void onFailure(Exception exception) {
                    log.warn("[ES] Index failed: {}", exception.getMessage());
                }
            });
        } catch (Exception e) {
            log.warn("[ES] Record failed: {}", e.getMessage());
        }
    }

    private String resolveIndex(LogEvent event) {
        return "log-" + event.type().name().toLowerCase() + "-" + YearMonth.now();
    }
}
