import java.util.HashMap;
import java.util.Map;

public class Main {

    public static void main(String[] args) {

        CacheMap<Integer, Integer> cm = new CacheMap<>(10, 10, new HashMap<>());

        System.out.printf("%b\n", cm.isEmpty());
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
        private final Map<K, V> db;
        private final Map<K, V> cache;

        CacheMap(final int cacheSize, final int timeoutSecs, final Map<K, V> db) {
            this.cacheSize = cacheSize;
            this.timeoutSecs = timeoutSecs;
            this.db = db;
            this.cache = new HashMap<>();
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
            this.cache.forEach(this.db::put);
            this.cache.clear();
        }
    }
}
