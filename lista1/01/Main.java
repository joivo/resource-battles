
import java.util.ArrayList;
import java.util.List;
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

    static class Counter extends Thread {
        private int maxValue;

        Counter(int maxValue) {
            this.maxValue = maxValue;
        }

        @Override
        public void run() {
            log(String.format("%s started.\n", this.getName()));
            counts(this.maxValue, this);
            log(String.format("%s exiting.\n", this.getName()));
        }
    }

    private static void counts(int maxValue, Thread t) {
        mutex.lock(t);
        log(String.format("Initial counter value %s\n", sharedCount));
        for (int i = 0; i < maxValue; i++) {
            sharedCount++;
        }
        log(String.format("Final result for the counter %s is %d\n", t.getName(), sharedCount));
        mutex.unlock(t);
    }

    static class Mutex {
        private Queue<Thread> waiters; // that should be enough to guarantee fairness
        private int flag;
        private int guard;

        Mutex() {
            this.flag = 0;
            this.guard = 0;
            this.waiters = new Queue<>();
        }

        void lock(Thread t) {
            while (testAndSet(flag, 1) == 1) {}

            if (flag == 0) {
                flag = 1;
                guard = 0;
            } else {
                this.waiters.enqueue(t);
                guard = 0;
                LockSupport.park(t);
            }
        }

        void unlock(Thread t) {
            while (testAndSet(guard, 1) == 1) {}
            if (this.waiters.isEmpty()) flag=0;
            else {
                this.waiters.take();
                LockSupport.unpark(t);
            }

            guard = 0;
        }
    }

    private static void log(String str) {
        System.out.print(str);
    }

    private static int testAndSet(int o, int s) {
        int temp = o;
        o = s;
        return temp;
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
