package learnrxjava.examples;

import java.util.ArrayList;

import rx.Observable;

public class ScanVsReduceExample {

    public static void main(String... args) {

        // reduce waits for the range to be processed
        Observable.range(0, 10).reduce(new ArrayList<>(), (list, i) -> {
            list.add(i);
            return list;
        }).forEach(System.out::println);

        System.out.println("... vs ...");

        // scan reports with every event
        Observable.range(0, 10).scan(new ArrayList<>(), (list, i) -> {
            list.add(i);
            return list;
        }).forEach(System.out::println);
    }
}
