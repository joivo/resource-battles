import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

public class Main {

    private static int sharedCount = 0;
    private final static Mutex mutex = new Mutex();

    public static void main(String... args) throws InterruptedException {
        // expected 200000;
        log(String.format("Expected: %d\n", 200000));
        log(String.format("Result: %d\n", testMutualExclusion(sharedCount, 10000, 20)));

        // expected 20000000;
        log(String.format("Expected: %d\n", 20000000));
        log(String.format("Result: %d\n", testMutualExclusion(sharedCount, 100000, 200)));

        // expected 2000000000;
        log(String.format("Expected: %d\n", 2000000000));
        log(String.format("Result: %d\n", testMutualExclusion(sharedCount, 1000000, 2000)));
    }

    private static int testMutualExclusion(int current, int maxValue, int nThreads) {
        int expected = maxValue * nThreads;
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < nThreads; i++) {
            threads.add(new Counter(maxValue));
        }

        for (int i = 0; i < nThreads; i++) {
            threads.get(i).start();
        }

        for (int i = 0; i < nThreads; i++) {
            try {
                threads.get(i).join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        assert current == expected;
        return expected;
    }

    static class Counter extends Thread {
        private final int maxValue;

        Counter(int maxValue) {
            this.maxValue = maxValue;
        }

        @Override
        public void run() {
            mutex.lock(this);
            for (int i = 0; i < maxValue; i++) {
                sharedCount++;
            }
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

        T peek() {
            return this.queue.get(0);
        }

        T take() {
            return this.queue.remove(0);
        }

        boolean isEmpty() {
            return this.queue.size() == 0;
        }
    }

    private static void log(String str) {
        System.out.print(str);
    }
}
