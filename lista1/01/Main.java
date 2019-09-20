import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

public class Main {

    private static int sharedCount = 0;
    private static Mutex mutex = new Mutex();

    public static void main(String... args) throws InterruptedException {
        final int maxValue = 1000;

        Thread c0 = new Counter(maxValue);
        Thread c1 = new Counter(maxValue);
        Thread c2 = new Counter(maxValue);

        c0.start();
        c1.start();
        c2.start();

        c0.join();
        c1.join();
        c2.join();

        log(String.format("Expected: %d\n", maxValue * 3));
        log(String.format("Result: %d\n", sharedCount));
    }

    private static void log(String str) {
        System.out.print(str);
    }

    static class Counter extends Thread {
        private final int maxValue;

        Counter(int maxValue) {
            this.maxValue = maxValue;
        }

        @Override
        public void run() {
            mutex.lock(this);
            log(String.format("Initial counter value %s\n", sharedCount));
            for (int i = 0; i < maxValue; i++) {
                sharedCount++;
            }
            log(String.format("Final result for the counter %s is %d\n", this.getName(), sharedCount));
            mutex.unlock(this);
        }
    }

    static class Mutex {
        private Queue<Thread> waiters; // that should be enough to guarantee fairness
        private AtomicInteger flag;
        private AtomicInteger guard;

        Mutex() {
            this.flag = new AtomicInteger(0);
            this.guard = new AtomicInteger(0);
            this.waiters = new Queue<>();
        }

        void lock(Thread t) {
            while (guard.getAndSet(1) == 1) {
                // if here, so spinning
            }

            if (flag.get() == 0) {
                flag.set(1); // lock acquired
                guard.set(0);
            } else {
                waiters.enqueue(t);
                guard.set(0);
                LockSupport.park();
            }

        }

        void unlock(Thread t) {
            while (guard.getAndSet(1) == 1) {
                // spinning...
            }

            if (waiters.isEmpty())
                flag.set(0);
            else
                LockSupport.unpark(waiters.take());

            guard.set(0);
        }
    }

    static class Queue<T> {
        private List<T> queue;

        Queue() {
            this.queue = new ArrayList<>();
        }

        void enqueue(T t) {
            this.queue.add(t);
        }

        // just show the head
        T peek() {
            return this.queue.get(0);
        }

        T take() {
            return this.queue.remove(0);
        }

        public boolean isEmpty() {
            return this.queue.size() == 0;
        }
    }
}
