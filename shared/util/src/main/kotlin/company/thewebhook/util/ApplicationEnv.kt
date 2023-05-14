package company.thewebhook.util

object ApplicationEnv {
    const val envPrefix = "TWC_CONFIG_"

    private val applicationEnvConfig =
        System.getenv().filter { it.key?.startsWith(envPrefix) == true && it.value !== null }
            as Map<String, String>

    fun get(key: String): String? {
        return applicationEnvConfig.getOrDefault(key, null)
    }

    fun getOrDefault(key: String, default: String): String {
        return this.get(key) ?: default
    }

    fun getAllMatchingPrefix(prefix: String, removePrefixFromKeys: Boolean): Map<String, String> {
        return applicationEnvConfig
            .filter { it.key.startsWith(prefix) && it.value.isNotEmpty() }
            .let { map ->
                if (removePrefixFromKeys) {
                    map.mapKeys { it.key.removePrefix(prefix) }
                } else {
                    map
                }
            }
    }
}
