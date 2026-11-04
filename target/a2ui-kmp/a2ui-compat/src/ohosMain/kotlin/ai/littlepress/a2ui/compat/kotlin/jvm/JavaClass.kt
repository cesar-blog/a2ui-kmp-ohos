package ai.littlepress.a2ui.compat.kotlin.jvm

import kotlin.reflect.KClass

public actual class JavaClass<T>(internal val kClass: KClass<*>) {
    override fun equals(other: Any?): Boolean = other is JavaClass<*> && other.kClass == kClass

    override fun hashCode(): Int = kClass.hashCode()

    override fun toString(): String = "class ${kClass.qualifiedName}"
}

public actual val <T : Any> T.javaClass: JavaClass<T>
    get() = JavaClass(this::class)

public actual val JavaClass<*>.simpleName: String
    get() = when (kClass) {
        Int::class -> "Integer"
        Char::class -> "Character"
        else -> kClass.simpleName ?: ""
    }
