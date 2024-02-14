package com.contentgrid.surveyor.application.exporter.cgapp;

import io.prometheus.metrics.exporter.common.PrometheusScrapeHandler;
import io.prometheus.metrics.exporter.servlet.jakarta.HttpExchangeAdapter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CgappMetricsController {

    private final PrometheusScrapeHandler prometheusScrapeHandler;

    @GetMapping("/metrics")
    public void getMetrics(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.debug("/metrics was scraped");
        HttpExchangeAdapter adapter = new HttpExchangeAdapter(request, response);
        prometheusScrapeHandler.handleRequest(adapter);

    }

}
