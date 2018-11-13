package learnrxjava.examples;

import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.schedulers.TestScheduler;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class UnitTesting {

    static Func1<Long, String> longToString = aLong -> aLong + " value";

    public static void main(String... args) {
        runTest();
        runReal();
    }

    private static void runTest() {
        TestScheduler testScheduler = Schedulers.test();
        TestSubscriber<String> testSubscriber = new TestSubscriber<>();

        composeStringEvery200ms(testScheduler, longToString).subscribe(testSubscriber);

        testScheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS);
        testSubscriber.assertReceivedOnNext(Arrays.asList("0 value"));

        testScheduler.advanceTimeTo(1000, TimeUnit.MILLISECONDS);
        testSubscriber.assertReceivedOnNext(Arrays.asList("0 value", "1 value", "2 value", "3 value", "4 value"));
    }

    private static void runReal() {

        composeStringEvery200ms(Schedulers.immediate(), longToString)
                .take(10)
                .subscribe(x -> System.out.println(x));
    }

    private static Observable<String> composeStringEvery200ms(Scheduler scheduler, Func1<Long, String> ordinalToString) {
        return Observable.interval(200, TimeUnit.MILLISECONDS, scheduler)
                .map(ordinalToString);
    }
}
