package company.thewebhook.messagestore.util

fun getPrefixEnv(prefix: String, removePrefixFromKeys: Boolean): Map<String, String> {
    return System.getenv()
        .filter { it.key.startsWith(prefix) && it.value.isNotEmpty() }
        .let { map ->
            if (removePrefixFromKeys) {
                map.mapKeys { it.key.removePrefix(prefix) }
            } else {
                map
            }
        }
}

fun getProducerConfigFromEnv() = getPrefixEnv("producer_config_", true)

fun getConsumerConfigFromEnv() = getPrefixEnv("consumer_config_", true)
