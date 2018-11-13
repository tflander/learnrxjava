package learnrxjava.examples;

import rx.Observable;

import java.util.concurrent.TimeUnit;

public class ErrorHandlingRetryWithBackoff {

    public static void main(String... args) {

//         * retry(n) can be used to immediately retry n times
        observableSometimesFails()
                .retry(3)
                .subscribe(System.out::println, t -> System.out.println("1) Error: " + t));

        System.out.println("");

//         * retryWhen allows custom behavior on when and if a retry should be done
        observableSometimesFails()
                .retryWhen(attempts -> {
                    return attempts.zipWith(Observable.range(1, 3), (n, i) -> i).flatMap(i -> {
                        System.out.println("2) delay retry by " + i + " second(s)");
                        return Observable.timer(i, TimeUnit.SECONDS);
                    })
                            .concatWith(Observable.error(new RuntimeException("Failed after 3 retries")));
                }).toBlocking().forEach(System.out::println);

    }

    private static Observable<String> observableAlwaysFails() {
        return Observable.create(s -> {
            System.out.println("subscribing");
            s.onError(new RuntimeException("always fails"));
        });
    }

    private static Observable<String> observableSometimesFails() {
        return Observable.create(s -> {
            System.out.println("subscribing...");
            if(Math.random() < 0.5) {
                System.out.println("...failed");
                s.onError(new RuntimeException("always fails"));
            } else {
                System.out.println("...succeeded");
                s.onNext("Success");
            }
        });
    }
}
