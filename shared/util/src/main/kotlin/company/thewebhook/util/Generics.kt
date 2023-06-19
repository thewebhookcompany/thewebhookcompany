package company.thewebhook.util

inline fun <reified T> Any?.asType() = if (this is T) this else null
