package company.thewebhook.util

import java.util.AbstractMap
import kotlinx.serialization.json.*

fun JsonElement.toAny(): Any? {
    return when (this) {
        is JsonObject -> this.toMapAny()
        is JsonArray -> this.toListAny()
        is JsonPrimitive -> this.toAny()
        JsonNull -> null
    }
}

fun JsonArray.toListAny(): List<Any?> {
    return this.map { it.toAny() }
}

fun JsonObject.toMapAny(): Map<String, Any?> {
    return this.map { AbstractMap.SimpleEntry(it.key, it.value.toAny()) }
        .associateBy({ it.key }, { it.value })
}

fun JsonPrimitive.toAny(): Any? {
    val v = this.toString()
    if (v.startsWith("\"") && v.endsWith("\"")) {
        return this.contentOrNull
    }
    return this.booleanOrNull ?: this.doubleOrNull ?: this.longOrNull
}
