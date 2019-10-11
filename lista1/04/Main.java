import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) {
        CacheMap<Integer, Integer> cm = new CacheMap<>(10, 10*1000, new HashMap<>());        
    }

    private static boolean validateArgs(String... args) {
        return args.length > 0 && args.length < 3;
    }

    private static void log(String str) {
        System.out.print(str);
    }

    static class CacheMap<K, V> {
        private static final int DEFAULT_POOL_THREAD_NUMBER = 1;
        private final int cacheSize;
        private final int timeoutSecs;
        private final Map<K, V> db;
        private final Map<K, V> cache;

        CacheMap(final int cacheSize, final int timeoutSecs, final Map<K, V> db) {
            this.cacheSize = cacheSize;
            this.timeoutSecs = timeoutSecs;
            this.db = db;
            this.cache = new HashMap<>();
            this.init();
        }

        void init() {
            final ManagerTimer managerTimer = new ManagerTimer(
                    Executors.newScheduledThreadPool(DEFAULT_POOL_THREAD_NUMBER));
            final CommitterRoutine committerRoutine = new CommitterRoutine();
            managerTimer.schedule(committerRoutine, this.timeoutSecs);
        }

        int size() {
            return this.db.size() + this.cache.size();
        }

        boolean isEmpty() {
            return size() == 0;
        }

        boolean containsKey(K k) {
            return this.cache.containsKey(k) ? this.cache.containsKey(k) : this.db.containsKey(k);
        }

        V get(K k) {
            if (this.cache.containsKey(k)) {
                return this.cache.get(k);
            } else {
                if (this.db.containsKey(k)) {
                    return putOnCache(k);
                }
            }
            return null;
        }

        Object put(K k, V v) {
            if (cacheIsFull()) {
                commit();
            }
            return this.cache.put(k, v);
        }

        V remove(K k) {
            if (this.cache.containsKey(k)) {
                return this.cache.remove(k);
            } else {
                if (this.db.containsKey(k)) {
                    return this.db.remove(k);
                }
            }
            return null;
        }

        void clear() {
            this.cache.clear();
            this.db.clear();
        }

        private V putOnCache(K k) {
            V v = this.db.remove(k);

            if (cacheIsFull()) {
                commit();
            }
            return this.cache.put(k, v);
        }

        private boolean cacheIsFull() {
            return this.cache.size() == this.cacheSize;
        }

        private void commit() {
            if (!this.cache.isEmpty()) {
                log("Commiting to the db.\n");
                this.cache.forEach(this.db::put);
                this.cache.clear();
            } else {
                log("Cache is empty, nothing to commit.\n");
            }
        }

        class CommitterRoutine implements Runnable {
            @Override 
            public void run() {                
                commit();
            }
        }
    }

    static class ManagerTimer {
        private static final int DEFAULT_INITIAL_DELAY_IN_MILLI = 1000;
        private ScheduledExecutorService executor;

        ManagerTimer(ScheduledExecutorService executor) {
            this.executor = executor;
        }

        /**
         * Time scheduler.
         *
         * @param routine      the procedure that should be executed in a certain time.
         * @param commitPeriod the periodic time
         */
        void schedule(final Runnable routine, int commitPeriod) {
            executor.scheduleWithFixedDelay(() -> {
                try {
                    routine.run();
                } catch (Throwable e) {
                    log("Error in execution of the commit!");
                }
            }, DEFAULT_INITIAL_DELAY_IN_MILLI, commitPeriod, TimeUnit.MILLISECONDS);
        }
    }
}