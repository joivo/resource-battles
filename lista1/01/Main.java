import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

public class Main {

    private final static Mutex mutex = new Mutex();
    private static int sharedCount = 0;

    public static void main(String... args) {
        final String usage = "usage:\nsh run <iterations> <n_threads> <counter_value>\n";

        if (validateArgs(args)) {
            if (args.length != 3) {
                log(usage);
            } else {
                int iterations = Integer.parseInt(args[0]);  
                int nThreads = Integer.parseInt(args[1]);
                int seed = Integer.parseInt(args[2]);
                int expected = seed * nThreads;
                
                for (int i = 0; i < iterations; i++) {                    
                    log(String.format("Expected: %d\n", expected));
                    log(String.format("Result: %d\n", testMutualExclusion(expected, seed, nThreads)));
                }

                System.exit(0);
            }
        } else {
            log(usage);
            System.exit(1);
        }
    }

    private static int testMutualExclusion(int expected, int seed, int nThreads) {
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < nThreads; i++) {
            threads.add(new Counter(seed));
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

        int current = sharedCount;
        sharedCount = 0;
        assert current == expected;
        return current;
    }

    private static void log(String str) {
        System.out.print(str);
    }

    static boolean validateArgs(String... args) {
        return args.length > 0 && validateIntLimits(args);

    }

    static boolean validateIntLimits(String ...args) {
        boolean result = true;
        for (String s : args) {
            if (Integer.parseInt(s) >= Integer.MAX_VALUE ||
                    Integer.parseInt(s) <= Integer.MIN_VALUE) {
                result = false;
                break;
            }
        }
        return result;
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
}
