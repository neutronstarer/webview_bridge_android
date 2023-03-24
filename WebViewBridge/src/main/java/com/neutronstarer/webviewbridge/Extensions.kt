package com.neutronstarer.webviewbridge

import java.util.WeakHashMap
import kotlin.collections.HashMap

private val EXTENSIONS by lazy { WeakHashMap<Any, MutableMap<String, Any>>() }

fun Any.addField(name: String, value: Any) {
    EXTENSIONS.getOrPut(this) { HashMap() }[name] = value
}

@Suppress("UNCHECKED_CAST")
fun <T> Any.getField(name: String): T? {
    return (EXTENSIONS[this]?.get(name)) as? T
}

@Suppress("UNCHECKED_CAST", "unused")
fun <T> Any.removeField(name: String): T? {
    return (EXTENSIONS[this]?.remove(name)) as? T
}