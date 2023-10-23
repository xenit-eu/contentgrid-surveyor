package com.contentgrid.surveyor.spi.storage;

import lombok.experimental.UtilityClass;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Mono;

@UtilityClass
class Util {

    public <T> Mono<T> onlyValue(Publisher<T> publisher) {
        return Mono.create(sink -> {
            publisher.subscribe(new BaseSubscriber<T>() {

                private T firstValue = null;

                @Override
                protected void hookOnCancel() {
                    upstream().cancel();
                }

                @Override
                protected void hookOnNext(T value) {
                    if (firstValue == null) {
                        firstValue = value;
                    } else {
                        upstream().cancel();
                        sink.error(new IllegalStateException(
                                "Multiple values emitted from publisher which should only emit a single value."));
                    }
                }

                @Override
                protected void hookOnSubscribe(Subscription subscription) {
                    subscription.request(2);
                }

                @Override
                protected void hookOnComplete() {
                    if (firstValue == null) {
                        sink.success();
                    } else {
                        sink.success(firstValue);
                    }
                }

                @Override
                protected void hookOnError(Throwable throwable) {
                    sink.error(throwable);
                }

            });

        });
    }

}
