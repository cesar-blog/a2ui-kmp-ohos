package ai.littlepress.a2ui.compat.java.util.concurrent

import ai.littlepress.a2ui.compat.java.util.concurrent.locks.ReentrantLock

public actual class CopyOnWriteArrayList<E> actual constructor() : AbstractList<E>() {
    private val lock = ReentrantLock(false)
    private var elements: List<E> = emptyList()

    private inline fun <T> locked(block: () -> T): T {
        lock.lock()
        try {
            return block()
        } finally {
            lock.unlock()
        }
    }

    private fun snapshot(): List<E> = locked { elements }

    actual override val size: Int
        get() = snapshot().size

    actual override fun get(index: Int): E = snapshot()[index]

    override fun iterator(): Iterator<E> = snapshot().iterator()

    override fun listIterator(): ListIterator<E> = snapshot().listIterator()

    override fun listIterator(index: Int): ListIterator<E> = snapshot().listIterator(index)

    public actual fun add(element: E): Boolean = locked {
        elements = elements + element
        true
    }

    public actual fun clear() {
        locked { elements = emptyList() }
    }
}
