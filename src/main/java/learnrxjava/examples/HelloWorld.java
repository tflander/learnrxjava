package learnrxjava.examples;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.Subscription;
import rx.exceptions.OnErrorNotImplementedException;
import rx.schedulers.Schedulers;

public class HelloWorld {

    public static void main(String[] args) {

        // Hello World
        Observable.create(subscriber -> {
            subscriber.onNext("Hello World!");
            subscriber.onCompleted();
        }).subscribe(System.out::println);
        // shorten by using helper method
        Observable<String> foo = Observable.just("Hello", "World!");

                foo.subscribe(x -> {
                    System.out.println("called " + x);
                });

        // add onError and onComplete listeners
        Observable.just("Hello World!")
                .subscribe(str -> {throw new IllegalStateException("force error");},
                        Throwable::printStackTrace,
                        () -> System.out.println("Done"));

        // expand to show full classes
        Observable.create(new OnSubscribe<String>() {

            @Override
            public void call(Subscriber<? super String> subscriber) {
                subscriber.onNext("Hello World!");
                subscriber.onCompleted();
            }

        }).subscribe(new Subscriber<String>() {

            @Override
            public void onCompleted() {
                System.out.println("Done");
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }

            @Override
            public void onNext(String t) {
                System.out.println(t);
            }

        });

        // add error propagation
        Observable.create(subscriber -> {
            try {
                subscriber.onNext("Hello World!");
                subscriber.onCompleted();
            } catch (OnErrorNotImplementedException e) {
                System.out.println("whoops");
//                subscriber.onError(e);
                e.printStackTrace();
            }
        }).subscribe(str -> {throw new IllegalStateException("forced");});

        // add concurrency (manually)
        System.out.println(getData());
        Observable.create(subscriber -> {
            new Thread(() -> {
                try {
                    subscriber.onNext(getData());
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }).start();
        }).subscribe(System.out::println, Throwable::printStackTrace, () -> {
            System.out.println("Done");
        });

        // add concurrency (using a Scheduler)
        Observable.create(subscriber -> {
            try {
                subscriber.onNext(getData());
                subscriber.onCompleted();
            } catch (Exception e) {
                subscriber.onError(e);
            }
        }).subscribeOn(Schedulers.io())
                .subscribe(System.out::println);

        // add operator
        Observable.create(subscriber -> {
            try {
                subscriber.onNext(getData());
                subscriber.onCompleted();
            } catch (Exception e) {
                subscriber.onError(e);
            }
        }).subscribeOn(Schedulers.io())
                .map(data -> ((String)data).length())
//                .map(data -> data + " --> at " + System.currentTimeMillis())
                .subscribe(System.out::println);

        // add error handling
        Observable.create(subscriber -> {
            try {
                subscriber.onNext(getData());
                subscriber.onCompleted();
            } catch (Exception e) {
                subscriber.onError(e);
            }
        }).subscribeOn(Schedulers.io())
                .map(data -> data + " --> at " + System.currentTimeMillis())
                .onErrorResumeNext(e -> Observable.just("Fallback Data"))
                .subscribe(System.out::println);

        // Todd spike
        Observable.create(subscriber -> {
            try {

                for(int i = 0; i < 22; ++i) {
                    subscriber.onNext(getAuthTokenOrNull());
                }
                subscriber.onCompleted();
            } catch (Exception e) {
                subscriber.onError(e);
            }
        }).subscribeOn(Schedulers.io())
//                .filter(data -> ((String)data).endsWith("0"))
//                .first()
                .onErrorResumeNext(e -> Observable.just("Fallback Data"))
                .subscribe(System.out::println);

        // infinite
        Observable.create(subscriber -> {
            int i = 0;
            while (!subscriber.isUnsubscribed()) {
                subscriber.onNext(i++);
            }
        }).take(10).subscribe(System.out::println);

        //Hello World
        Observable.create(subscriber -> {
            throw new RuntimeException("failed!");
        }).onErrorResumeNext(throwable -> {
            return Observable.just("fallback value");
        }).subscribe(System.out::println);

        Observable.create(subscriber -> {
            throw new RuntimeException("failed!");
        }).onErrorResumeNext(Observable.just("fallback value 1", "fallback value 2"))
                .subscribe(System.out::println);

        Observable.create(subscriber -> {
            throw new RuntimeException("failed!");
        }).onErrorReturn(throwable -> {
            return "fallback value";
        }).subscribe(System.out::println);

        Observable.create(subscriber -> {
            throw new RuntimeException("failed!");
        }).retryWhen(attempts -> {
            return attempts.zipWith(Observable.range(1, 3), (throwable, i) -> i)
                    .flatMap(i -> {
                        System.out.println("delay retry by " + i + " second(s)");
                        return Observable.timer(i, TimeUnit.SECONDS);
                    }).concatWith(Observable.error(new RuntimeException("Exceeded 3 retries")));
        })
                .subscribe(System.out::println, t -> t.printStackTrace());

        try {
            Thread.sleep(20000);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
    }

    private static String getAuthTokenOrNull() {
        System.out.println(".");
        return Thread.currentThread().getName() + " " + Math.round(Math.random() * 3);
    }

    private static String getData() {
//        throw new IllegalStateException("force");
        return Thread.currentThread().getName() + " Got Data!";
    }

}
