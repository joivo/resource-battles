package main;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

public class Main {

    public static void main(String... args) {
        int count = 0;
        final int maxValue = 100000;

        Counter firstCounter = new Counter(count, "c0",maxValue);
        Counter secondCounter = new Counter(count, "c1",maxValue);
        Counter thirdCounter = new Counter(count, "c2",maxValue);

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

    static class Counter implements Runnable {

        private int count;
        private String counterId;
        private Mutex mutex;
        private int maxValue;

        Counter(int count, String counterId, int maxValue) {
            this.mutex = new Mutex();
            this.count = count;
            this.counterId = counterId;
            this.maxValue = maxValue;
        }

        @Override
        public void run() {
            this.mutex.lock();
            counts();
            this.mutex.unlock();
        }

        private void counts() {
            log("The value of the shared variable is: " +
                    this.count + "to the counter with id: " + counterId);
            for (int i = 0; i <= this.maxValue; i++) {
                this.count++;
            }
        }
    }

    static class Mutex {
        private Queue waiters; // that should be enough to guarantee fairness
        private AtomicBoolean flag;

        Mutex() {
            this.flag = new AtomicBoolean(false);
            this.waiters = new Queue();
        }

        void lock() {
            boolean interrupted = false;
            Thread current = Thread.currentThread();
            this.waiters.enqueue(current);

            Thread head = this.waiters.peek();

            while ( (head != current) || flag.compareAndSet(false, true)) {
                    LockSupport.park(this);
                    if (Thread.interrupted()) interrupted = true;
            }

            this.waiters.take();

            if (interrupted) current.interrupt();
        }

        void unlock() {
            flag.set(false);
            LockSupport.unpark(this.waiters.peek());
        }
    }

    static class Queue {
        private List<Thread> queue;

        Queue() {
            this.queue = new ArrayList<>();
        }

        // enqueue a new thread
        void enqueue(Thread t) {
            this.queue.add(t);
        }

        // just show the head
        Thread peek() {
            return this.queue.get(0);
        }

        // remove and returns
        Thread take() {
            Thread head = this.queue.get(0);
            this.queue.remove(0);
            return head;
        }
    }

    private static void log(String str) {
        System.out.println(str);
    }
}
