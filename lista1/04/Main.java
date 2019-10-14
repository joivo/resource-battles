import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

    private static boolean validateArgs(String... args) {
        return args.length > 0 && args.length < 3;
    }

    private static void log(String str) {
        long timestamp = Instant.now().getEpochSecond();
        Date date = new java.util.Date(timestamp * 1000L);
        SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("GMT-3"));
        System.out.print(sdf.format(date) + " - " + str + "\n");
    }

    public static void main(String[] args) throws InterruptedException {
        CacheMap<Integer, Integer> cm = new CacheMap<>(10, 10, new HashMap<>());
        log("" + (cm.size() == 0));

        Collection<Integer> items = Collections.synchronizedCollection(new LinkedList<>());
        List<Thread> putWorkers = new ArrayList<>();
        Random random = new Random();

        int nThreads = 11;

        for (int i = 0; i <= nThreads; i++) {
            Integer key = random.nextInt();
            items.add(key);
            putWorkers.add(new PutWorker<>("PutWorker-" + i, cm, key, key - 1));
        }

        executeThreads(putWorkers, nThreads);

        log("" + (cm.size() == 11));
        log("" + (cm.cache.size() == 1));
        log("" + (cm.db.size() == 10));

        Thread.sleep(10*1000);

        log("" + (cm.cache.size() == 1));
        log("" + (cm.db.size() == 10));

        Collection<Integer> items2 = Collections.synchronizedCollection(new LinkedList<>());
        List<Thread> putWorkers2 = new ArrayList<>();        

        int nThreads2 = 9;

        for (int i = 0; i <= nThreads2; i++) {
            Integer key = random.nextInt();
            items2.add(key);
            putWorkers2.add(new PutWorker<>("PutWorker-" + i, cm, key, key - 1));
        }

        executeThreads(putWorkers2, nThreads2);

        log("" + (cm.size() == 20));

        log("" + (cm.cache.size() == 10));

        log("" + (cm.db.size() == 10));

        Thread.sleep(10*1000); 

        log("" + (cm.size() == 20));

        log("" + (cm.cache.size() == 0));

        log("" + (cm.db.size() == 20));

    }

    private static void executeThreads(List<Thread> threads, int nThreads) {

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
    }

    /**
     * This class describes the data structure that defines a memory cache for a given database, with a commit routine.
     * The database supported here is a key-value DB.
     *
     * @param <K> The generic type of the key of the data.
     * @param <V> The generic type of the value of the data.
     */
    static class CacheMap<K, V> {
        private static final int DEFAULT_POOL_THREAD_NUMBER = 1;
        private final int cacheSize;
        private final int timeoutMilli;
        private final Map<K, V> db;
        private final Map<K, V> cache;

        CacheMap(final int cacheSize, final int timeoutSecs, final Map<K, V> db) {
            this.cacheSize = cacheSize;
            this.timeoutMilli = timeoutSecs * 1000;
            this.db = db;
            this.cache = new HashMap<>(cacheSize);
            this.init();
        }

        void init() {
            final ManagerTimer managerTimer = new ManagerTimer(
                    Executors.newScheduledThreadPool(DEFAULT_POOL_THREAD_NUMBER));
            managerTimer.schedule(new Committer(), this.timeoutMilli);
        }

        synchronized int size() {
            return this.db.size() + this.cache.size();
        }

        synchronized boolean isEmpty() {
            return size() == 0;
        }

        synchronized boolean containsKey(K k) {
            return this.cache.containsKey(k) ? this.cache.containsKey(k) : this.db.containsKey(k);
        }

        synchronized V get(K k) throws InterruptedException {
            if (this.cache.containsKey(k)) {
                return this.cache.get(k);
            } else {
                if (this.db.containsKey(k)) {
                    if (!cacheIsFull()) {
                        this.wait();
                        return putOnCache(k);
                    }
                }
            }
            return null;
        }

        V put(K k, V v) {
            if (cacheIsFull()) {
                commit();
            }
            synchronized (this.cache) {
                return this.cache.put(k, v);
            }
        }

        synchronized V remove(K k) {
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
            synchronized (this) {
                this.cache.clear();
                this.db.clear();
            }
        }

        private synchronized V putOnCache(K k) {
            V v = this.db.remove(k);
            return this.cache.put(k, v);
        }

        private synchronized boolean cacheIsFull() {
            return this.cache.size() == this.cacheSize;
        }

        private synchronized void commit() {
            if (this.cache.size() > 1) {
                log("Committing to the db.");
                this.cache.forEach(this.db::put);
                this.cache.clear();
                this.notifyAll();
            } else {
                log("Cache already has space, nothing to commit.");
            }
        }

        class Committer implements Runnable {
            @Override
            public void run() {
                commit();
            }
        }
    }

    static class PutWorker<K, V> extends Thread {
        private CacheMap<K, V> cache;
        private K k;
        private V v;

        PutWorker(String workerName, CacheMap<K, V> cache, K k, V v) {
            super(workerName);
            this.cache = cache;
            this.k = k;
            this.v = v;
        }

        @Override
        public void run() {
            this.cache.put(this.k, v);
        }
    }

    static class RemoveWorker<K, V> implements Runnable {
        private CacheMap<K, V> cache;
        private K k;

        RemoveWorker(CacheMap<K, V> cache, K k) {
            this.cache = cache;
            this.k = k;
        }

        @Override
        public void run() {
            this.cache.remove(this.k);
        }
    }

    static class GetWorker<K, V> implements Runnable {
        private CacheMap<K, V> cache;
        private K k;

        GetWorker(CacheMap<K, V> cache, K k) {
            this.cache = cache;
            this.k = k;
        }

        @Override
        public void run() {
            try {
                this.cache.get(this.k);
            } catch (InterruptedException e) {
                e.printStackTrace();
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
         * @param routine      the procedure that must be performed from time to time.
         * @param commitPeriod the periodic time that the routine execute.
         */
        void schedule(final Runnable routine, long commitPeriod) {
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