package org.example.springadminv2.global.log.config;

import java.util.List;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Configuration
@ConditionalOnProperty(name = "log.event.elasticsearch.enabled", havingValue = "true")
@ConfigurationProperties("log.event.elasticsearch")
public class ElasticsearchConfig {

    private List<String> uris = List.of("http://localhost:9200");

    @Bean
    public RestClient elasticsearchRestClient() {
        HttpHost[] hosts = uris.stream().map(HttpHost::create).toArray(HttpHost[]::new);
        return RestClient.builder(hosts).build();
    }
}
