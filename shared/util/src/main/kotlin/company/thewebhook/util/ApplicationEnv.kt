package company.thewebhook.util

object ApplicationEnv {
    const val envPrefix = "TWC_CONFIG_"

    private val applicationEnvConfig =
        System.getenv().filter { it.key?.startsWith(envPrefix) == true && it.value !== null }
            as Map<String, String>

    fun get(key: String): String? {
        return applicationEnvConfig.getOrDefault(key, null)
    }

    fun get(key: String, default: String): String {
        val v = this.get(key)
        return (if (v?.isNotBlank() == true) v else null) ?: default
    }
}
