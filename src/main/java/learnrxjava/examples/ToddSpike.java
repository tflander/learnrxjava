package learnrxjava.examples;


import rx.Observable;
import rx.schedulers.Schedulers;

public class ToddSpike {

    public static void main(String[] args) throws InterruptedException {

        multiSubScribe();

    }

    private static void multiSubScribe() throws InterruptedException {
        Observable<String> server_a = createAuthObservableWithDelay("Server A", 6000);
        Observable<String> server_b = createAuthObservableWithDelay("Server B", 30);

        Observable.merge(server_a.subscribeOn(Schedulers.io()), server_b.subscribeOn(Schedulers.io()))
                .take(1)
                .toBlocking()
                .forEach(System.out::println);
    }

    private static Observable<String> createAuthObservableWithDelay(String serverName, int delay) {
        Observable<String> authCall = Observable.create(subscriber -> {
            try {
                System.out.println("calling auth against " + serverName + "...");
                Thread.sleep(delay);
                System.out.println(serverName + " returned auth.");
            } catch (InterruptedException e) {
                System.out.println("aborting auth from " + serverName);
            }
            subscriber.onNext(serverName);
            subscriber.onCompleted();

        });
        return authCall;
    }

    private static Observable<String> createAuthObservable(String serverName) {
        return createAuthObservableWithDelay(serverName, 300);
    }

}
