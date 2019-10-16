import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) {

        if (args.length == 0) {
            log("Running test mode");
            log("Cache size: 10");
            log("Timeout db commit: 10");
            final CacheMap<String, Double> cm = new CacheMap<>(10, 10, new HashMap<>());
            Collection<String> keysInserted = Collections.synchronizedCollection(new LinkedList<>());

            testPut(cm, keysInserted);

            testGet(cm, keysInserted);

            testRemove(cm, keysInserted);

            log("--- Tests finished ---");
            System.exit(0);
        } else {
            log("No parameters supported yet");
            System.exit(1);
        }
    }

    private static void testPut(CacheMap<String, Double> cm, Collection<String> workersKeys) {
        log("--- Put tests ---");
        // set up
        long waitTime = 10 * 1000;

        // exercise
        doPutWork(11, workersKeys, cm);

        // verify
        log("Test 01 - Result: " + (cm.size() == 11));
        log("Test 02 - Result: " + (cm.cache.size() == 1));
        log("Test 03 - Result: " + (cm.db.size() == 10));
        log("Test 04 - Result: " + (workersKeys.size() == 11));

        try {
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        log("Test 05 - Result: " + (cm.cache.size() == 1));
        log("Test 06 - Result: " + (cm.db.size() == 10));

        // exercise
        doPutWork(9, workersKeys, cm);
        log("Test 07 - Result: " + (workersKeys.size() == 20));

        // verify
        log("Test 08 - Result: " + (cm.size() == 20));
        log("Test 09 - Result: " + (cm.cache.size() == 10));
        log("Test 10 - Result: " + (cm.db.size() == 10));

        try {
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        log("Test 11 - Result: " + (cm.size() == 20));

        log("Test 12 - Result: " + (cm.cache.size() == 0));

        log("Test 13 - Result: " + (cm.db.size() == 20));

        cm.clear();
        workersKeys.clear();
    }

    private static void doPutWork(int nWorkers, Collection<String> workersKeys, CacheMap<String, Double> cm) {
        List<Thread> putWorkers = new ArrayList<>();
        for (int i = 0; i < nWorkers; i++) {
            String key = UUID.randomUUID().toString();

            Double value = Math.random();

            workersKeys.add(key);
            putWorkers.add(new PutWorker<>("PutWorker-" + i, cm, key, value));
        }

        doWork(putWorkers);
    }

    private static void testGet(CacheMap<String, Double> cm, Collection<String> workersKeys) {
        log("--- Get tests ---");
        List<Thread> getWorkers = new LinkedList<>();

        log("Test 14 - Result: " + (cm.isEmpty()));
        log("Test 15 - Result: " + (workersKeys.isEmpty()));

        doPutWork(20, workersKeys, cm);

        for (String workerKey : workersKeys) {
            if (cm.containsKey(workerKey)) {
                getWorkers.add(new GetWorker<>("GetWorker-" + workerKey, cm, workerKey));
            }
        }

        doWork(getWorkers);

        log("Test 16 - Result: " + (cm.size() == 20));

        log("Test 17 - Result: " + (cm.cache.size() == 10));

        log("Test 18 - Result: " + (cm.db.size() == 10));

        try {
            Thread.sleep(10 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        log("Test 19 - Result: " + (cm.size() == 20));

        log("Test 20 - Result: " + (cm.cache.size() == 0));

        log("Test 21 - Result: " + (cm.db.size() == 20));

        cm.clear();
        workersKeys.clear();
    }

    private static void testRemove(CacheMap<String, Double> cm, Collection<String> workersKeys) {
        log("--- Remove tests ---");

        log("Test 22 - Result: " + (cm.isEmpty()));
        log("Test 23 - Result: " + (workersKeys.isEmpty()));

        doPutWork(22, workersKeys, cm);

        log("Test 24 - Result: " + (cm.size() == 22));
        log("Test 25 - Result: " + (cm.cache.size() == 2));
        log("Test 26 - Result: " + (cm.db.size() == 20));

        doRemoveWork(22, workersKeys, cm);

        log("Test 27 - Result: " + (cm.isEmpty()));
        log("Test 28 - Result: " + (workersKeys.isEmpty()));

        doPutWork(30, workersKeys, cm);

        log("Test 29 - Result: " + (cm.size() == 30));
        log("Test 30 - Result: " + (cm.cache.size() == 10));
        log("Test 31 - Result: " + (cm.db.size() == 20));

        try {
            Thread.sleep(10 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        log("Test 32 - Result: " + (cm.size() == 30));
        log("Test 33 - Result: " + (cm.cache.size() == 0));
        log("Test 34 - Result: " + (cm.db.size() == 30));

        doRemoveWork(20, workersKeys, cm);

        log("Test 35 - Result: " + (cm.size() == 10));
        log("Test 36 - Result: " + (cm.cache.size() == 0));
        log("Test 37 - Result: " + (cm.db.size() == 10));

        doPutWork(30, workersKeys, cm);

        log("Test 38 - Result: " + (cm.size() == 40));
        log("Test 39 - Result: " + (cm.cache.size() == 10));
        log("Test 40 - Result: " + (cm.db.size() == 30));

        try {
            Thread.sleep(10 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        log("Test 41 - Result: " + (cm.size() == 40));
        log("Test 42 - Result: " + (cm.cache.size() == 0));
        log("Test 43 - Result: " + (cm.db.size() == 40));

        doRemoveWork(40, workersKeys, cm);

        log("Test 44 - Result: " + (cm.isEmpty()));
        log("Test 45 - Result: " + (workersKeys.isEmpty()));
    }

    private static void doRemoveWork(int nWorkers, Collection<String> workersKeys, CacheMap<String, Double> cm) {
        List<Thread> removeWorkers = new ArrayList<>();
        List<String> removedKeys = new ArrayList<>();

        int i = 0;
        for (String workerKey : workersKeys) {
            if (i < nWorkers) {
                i++;
                removeWorkers.add(new RemoveWorker<>("RemoveWorker-" + workerKey, cm, workerKey));
                removedKeys.add(workerKey);
            } else
                break;
        }

        for (String workerKey : removedKeys)
            workersKeys.remove(workerKey);

        doWork(removeWorkers);
    }

    private static void doWork(List<Thread> workers) {

        for (int i = 0; i < workers.size(); i++) {
            workers.get(i).start();
        }

        for (int i = 0; i < workers.size(); i++) {
            try {
                workers.get(i).join();
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

        boolean containsKey(K k) {
            synchronized (this) {
                return this.cache.containsKey(k) ? this.cache.containsKey(k) : this.db.containsKey(k);
            }
        }

        V get(K k) throws InterruptedException {
            synchronized (this) {
                if (this.cache.containsKey(k)) {
                    return this.cache.get(k);
                } else {
                    if (this.db.containsKey(k)) {
                        if (cacheIsFull()) {
                            this.wait();
                            return putOnCache(k);
                        }
                    }
                }
                return null;
            }
        }

        V put(K k, V v) {
            synchronized (this) {
                if (cacheIsFull()) {
                    commit();
                }
                this.cache.put(k, v);
                return this.cache.get(k);
            }
        }

        V remove(K k) {
            synchronized (this) {
                if (this.cache.containsKey(k)) {
                    return this.cache.remove(k);
                } else {
                    if (this.db.containsKey(k)) {
                        return this.db.remove(k);
                    }
                }
                return null;
            }
        }

        void clear() {
            synchronized (this) {
                this.cache.clear();
                this.db.clear();
            }
        }

        private V putOnCache(K k) {
            synchronized (this) {
                V v = this.db.remove(k);
                this.cache.put(k, v);
                return this.cache.get(k);
            }
        }

        private boolean cacheIsFull() {
            synchronized (this) {
                return this.cache.size() == this.cacheSize;
            }
        }

        private void commit() {
            synchronized (this) {
                if (this.cache.size() > 1) {
                    log("Committing to the db");
                    this.cache.forEach(this.db::put);
                    this.cache.clear();
                    this.notifyAll();
                } else {
                    log("Cache already has space, nothing to commit");
                }
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
        private final CacheMap<K, V> cacheMap;
        private K k;
        private V v;

        PutWorker(String workerName, final CacheMap<K, V> cacheMap, K k, V v) {
            super(workerName);
            this.cacheMap = cacheMap;
            this.k = k;
            this.v = v;
        }

        @Override
        public void run() {
            log("Worker " + this.getName() + " running");
            V insertedValue = this.cacheMap.put(this.k, this.v);
            if (Objects.isNull(insertedValue)) {
                throw new RuntimeException("Returned value for the key " + this.k + " is null");
            }
        }
    }

    static class RemoveWorker<K, V> extends Thread {
        private CacheMap<K, V> cacheMap;
        private K k;

        RemoveWorker(String workerName, CacheMap<K, V> cacheMap, K k) {
            super(workerName);
            this.cacheMap = cacheMap;
            this.k = k;
        }

        @Override
        public void run() {
            log("Worker " + this.getName() + " running");
            V v = this.cacheMap.remove(this.k);
            ;
            if (Objects.isNull(v)) {
                log("Worker " + this.getName() + " could not remove the value");
            }
        }
    }

    private static class GetWorker<K, V> extends Thread {
        private final CacheMap<K, V> cacheMap;
        private K k;

        GetWorker(String workerName, final CacheMap<K, V> cache, K k) {
            super(workerName);
            this.cacheMap = cache;
            this.k = k;
        }

        @Override
        public void run() {
            try {
                log("Worker " + this.getName() + " running");
                V v = this.cacheMap.get(this.k);
                if (Objects.isNull(v)) {
                    log("Worker " + this.getName() + " did not find the value sought");
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
                    log("Error in execution of the commit");
                }
            }, DEFAULT_INITIAL_DELAY_IN_MILLI, commitPeriod, TimeUnit.MILLISECONDS);
        }
    }

    private static void log(String str) {
        long timestamp = Instant.now().getEpochSecond();
        Date date = new java.util.Date(timestamp * 1000L);
        SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("GMT-3"));
        System.out.print(sdf.format(date) + " - " + str + "\n");
    }
}