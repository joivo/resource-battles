
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
        final String usage = "usage:\nsh run <cache_size> <timeout_secs> <n_threads> \n";

        if (validateArgs(args)) {
            if (args.length != 3) {
                log(usage);
            } else {
                int cacheSize = Integer.parseInt(args[0]);
                int timeoutSecs = Integer.parseInt(args[1]);
                int nThreads = Integer.parseInt(args[2]);

                CacheMap cm = new CacheMap<Integer, Integer>(cacheSize, timeoutSecs, new HashMap<>());

                List<Thread> threads = new ArrayList<>();

                for (int i = 0; i < nThreads; i++) {
                    threads.add(new Thread());
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

                System.exit(0);
            }
        } else {
            log(usage);
            System.exit(1);
        }
    }

    private static boolean validateArgs(String... args) {
        return args.length > 0 && args.length < 3;
    }
    private static void log(String str) {
        System.out.print(str);
    }

    static class CacheMap<K, V> {
        private final int cacheSize;
        private final int timeoutSecs;
        private final Map<K, V> dbMap;

        CacheMap(final int cacheSize, final int timeoutSecs, final Map<K, V> dbMap) {
            this.cacheSize = cacheSize;
            this.timeoutSecs = timeoutSecs;
            this.dbMap = dbMap;
        }

        public int size() {
            return 0;
        }

        public boolean isEmpty() {
            return false;
        }

        public boolean containsKey(Object o) {
            return false;
        }

        public Object get(Object o) {
            return null;
        }

        public Object put(K o, V o2) {
            synchronized (dbMap) {
                this.dbMap.put(o, o2);
                return null;
            }
        }

        public Object remove(Object o) {
            return null;
        }

        public void clear() {

        }
    }
}

