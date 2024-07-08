package util

import kotlin.enums.EnumEntries

class CompleteEnumMap<T : Enum<T>, E>(
    enumEntries: EnumEntries<T>,
    createEmptyElement: () -> MutableCollection<E>
) {
    private val map: Map<T, MutableCollection<E>> = enumEntries.associateWith { createEmptyElement() }


    fun getEntry(key: T): Collection<E> {
        return map[key]!!
    }

    fun addAll(newMap: Map<T, E>) {
        newMap.forEach { (key, value) ->
            map[key]!!.add(value)
        }
    }

    fun add(key: T, value: E) {
        map[key]!!.add(value)
    }

}
