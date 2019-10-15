import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {    

    public static void main(String[] args) throws InterruptedException {
        CacheMap<String, Integer> cm = new CacheMap<>(10, 10, new HashMap<>());
        Collection<String> keysInserted = Collections.synchronizedCollection(new LinkedList<>());
        Random random = new Random();

        testPut(cm, random, keysInserted);

        testGet(cm, keysInserted);
    }

    private static void testGet(CacheMap<String, Integer> cm, Collection<String> keys) {
        log("" + (!cm.isEmpty()));

        List<Thread> getWorkers = new LinkedList<>();

        log("" + (!keys.isEmpty()));

        for (String key : keys) {
            getWorkers.add(new GetWorker<>("GetWorker-" + key, cm, key));
        }

        executeThreads(getWorkers, keys.size());
    }

    private static void testPut(CacheMap<String, Integer> cm, Random r, Collection<String> workersKeys) {

        List<Thread> putWorkers = new ArrayList<>();
        log("" + (cm.size() == 0));

        int nThreads = 11;
        long waitTime = 10 * 1000;

        generatePutWorkers(nThreads, r, workersKeys, putWorkers, cm);
        executeThreads(putWorkers, nThreads);

        log("" + (cm.size() == 11));
        log("" + (cm.cache.size() == 1));
        log("" + (cm.db.size() == 10));

        try {
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        log("" + (cm.cache.size() == 1));
        log("" + (cm.db.size() == 10));

        int nThreads2 = 9;
        List<Thread> putWorkers2 = new ArrayList<>();
        generatePutWorkers(nThreads2, r, workersKeys, putWorkers2, cm);

        executeThreads(putWorkers2, nThreads2);

        log("" + (cm.size() == 20));

        log("" + (cm.cache.size() == 10));

        log("" + (cm.db.size() == 10));

        try {
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        log("" + (cm.size() == 20));

        log("" + (cm.cache.size() == 0));

        log("" + (cm.db.size() == 20));
    }

    private static void generatePutWorkers(int nThreads, Random r, Collection<String> workersKeys,
            List<Thread> putWorkers, CacheMap<String, Integer> cm) {
        for (int i = 0; i <= nThreads; i++) {
            Integer value = Math.abs(r.nextInt());
            String key = UUID.randomUUID().toString();
            workersKeys.add(key);
            putWorkers.add(new PutWorker<String, Integer>("PutWorker-" + i, cm, key, value));
        }
    }

    private static void generateRemoveWorkers(int nThreads, Random r, Collection<Integer> workersKeys,
            List<Thread> removeWorkers, CacheMap<Integer, Integer> cm) {
        for (int i = 0; i <= nThreads; i++) {
            Integer key = r.nextInt();
            workersKeys.add(key);
            removeWorkers.add(new PutWorker<>("RemoveWorker-" + i, cm, key, key - 1));
        }
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
     * This class describes the data structure that defines a memory cache for a
     * given database, with a commit routine. The database supported here is a
     * key-value DB.
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
            log("Worker " + this.getName() + " running.");
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

    static class GetWorker<K, V> extends Thread {
        private CacheMap<K, V> cache;
        private K k;

        GetWorker(String workerName, CacheMap<K, V> cache, K k) {
            super(workerName);
            this.cache = cache;
            this.k = k;
        }

        @Override
        public void run() {
            try {
                log("Worker " + this.getName() + " running.");
                V v = this.cache.get(this.k);
                if (Objects.isNull(v)) {
                    throw new RuntimeException();
                }
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
}