package main;

import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String... args) {
        int count = 0;

        Counter firstCounter = new Counter(count, "c0");
        Counter secondCounter = new Counter(count, "c1");
        Counter thirdCounter = new Counter(count, "c2");

        Thread t0 = new Thread(firstCounter, "thread-c0");
        Thread t1 = new Thread(secondCounter, "thread-c1");
        Thread t2 = new Thread(thirdCounter, "thread-c2");

        t0.start();
        t1.start();
        t2.start();

        try {
            t0.join();
            t1.join();
            t2.join();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void log(String str) {
        System.out.println(str);
    }

    static class Counter implements Runnable {

        private int count;
        private String counterId;

        Counter(int count, String counterId) {
            this.count = count;
            this.counterId = counterId;
        }


        @Override
        public void run() {
            for (int i = 0; i <= 1000; i++) {
                log("The value of the shared variable is: " +
                        this.count + "to the counter with id: " + counterId);
                this.count++;
            }
        }
    }

    class Mutex {
        private List<Integer> waiting;
        private int flag;
        private int guard;

        Mutex() {

            this.waiting = new ArrayList<>();
        }

        void lock() {

        }

        void unlock() {

        }
    }
}
