package ai.littlepress.a2ui.compat.java.util.concurrent

import ai.littlepress.a2ui.compat.java.util.concurrent.locks.ReentrantLock

public actual class ConcurrentHashMap<K : Any, V : Any> actual constructor() {
    private val lock = ReentrantLock(false)
    private val map = HashMap<K, V>()

    private inline fun <T> locked(block: () -> T): T {
        lock.lock()
        try {
            return block()
        } finally {
            lock.unlock()
        }
    }

    public actual operator fun get(key: K): V? = locked { map[key] }

    public actual operator fun set(key: K, value: V) {
        locked { map[key] = value }
    }

    public actual fun computeIfAbsent(key: K, mappingFunction: (K) -> V): V =
        locked { map.getOrPut(key) { mappingFunction(key) } }

    public actual fun compute(key: K, remappingFunction: (K, V?) -> V?): V? = locked {
        val value = remappingFunction(key, map[key])
        if (value == null) map.remove(key) else map[key] = value
        value
    }

    public actual fun remove(key: K): V? = locked { map.remove(key) }

    public actual fun clear() {
        locked { map.clear() }
    }
}
