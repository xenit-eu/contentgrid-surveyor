package com.contentgrid.surveyor.drivers.billing;

import com.contentgrid.surveyor.api.metrics.AggregateBillingMetrics;
import com.contentgrid.surveyor.api.metrics.AggregateBillingMetrics.AggregateBillingMetricsCommand;
import com.contentgrid.surveyor.spi.resources.LinkedMeasurements;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class BillingMetricsController {

    private final AggregateBillingMetrics aggregateBillingMetrics;

    @GetMapping("/metrics/billing")
    public Mono<CollectionModel<BillingRecordRepresentationModel>> billingMonthMetricsJson(
            @RequestParam int year,
            @RequestParam int month
    ) {
        var from = OffsetDateTime.of(year, month, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        var to = from.plusMonths(1);

        var flux = aggregateBillingMetrics.findMetricsForBilling(new AggregateBillingMetricsCommand(
                from.toInstant(), to.toInstant()));

        return flux
                .map(BillingRecordRepresentationModel::from)
                .collectList()
                .map(CollectionModel::of);
    }

    @GetMapping(value = "/metrics/billing.csv", produces = {"text/csv"})
    public ResponseEntity<Flux<BillingCsvRecordModel>> billingMonthMetricsCsv(
            @RequestParam int year,
            @RequestParam int month
    ) {
        var from = OffsetDateTime.of(year, month, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        var to = from.plusMonths(1);

        var flux = aggregateBillingMetrics.findMetricsForBilling(new AggregateBillingMetricsCommand(
                from.toInstant(), to.toInstant()));

        return ResponseEntity.ok(flux.collectList().flatMapMany(l -> {
            l.sort(new LMComparator());
            return Flux.fromIterable(l);
        }).map(BillingCsvRecordModel::from));

    }

    private static class LMComparator implements Comparator<LinkedMeasurements> {
        @Override
        public int compare(LinkedMeasurements a, LinkedMeasurements b) {
            // compare org
            var orgCompare = a.linkage().getOrgRef().compareTo(b.linkage().getOrgRef());
            if (orgCompare != 0) {
                return orgCompare;
            }

            // otherwise compare project
            var projectCompare = a.linkage().getProjectRef().compareTo(b.linkage().getProjectRef());
            if (projectCompare != 0) {
                return projectCompare;
            }

            // otherwise compare app
            return a.linkage().getApplicationRef().compareTo(b.linkage().getApplicationRef());
        }
    }
}
