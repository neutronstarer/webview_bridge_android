package com.neutronstarer.webviewbridge

import org.json.JSONArray
import org.json.JSONObject

fun JSONObject.toMap(): Map<String, Any> {
    val keys = this.keys()
    val map = mutableMapOf<String, Any>()
    while (keys.hasNext()) {
        val key = keys.next()
        when (val value = this.get(key)) {
            is JSONObject -> map[key] = value.toMap()
            is JSONArray -> map[key] = value.toList()
            else -> map[key] = value
        }
    }
    return map
}

fun JSONArray.toList(): List<Any> {
    val list = mutableListOf<Any>()
    for (i in 0 until this.length()) {
        when (val value = this.get(i)) {
            is JSONObject -> list.add(value.toMap())
            is JSONArray -> list.add(value.toList())
            else -> list.add(value)
        }
    }
    return list
}