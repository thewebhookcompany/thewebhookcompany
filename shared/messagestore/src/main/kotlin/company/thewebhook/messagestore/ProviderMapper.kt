package company.thewebhook.messagestore

import company.thewebhook.util.ApplicationEnv
import company.thewebhook.util.ConfigException
import company.thewebhook.util.toMapAny
import kotlinx.serialization.json.*

data class ProviderConfig(
    val provider: Provider,
    val internalConfig: Map<String, Any?>,
    val topics: Set<String>,
)

class ProviderMapper {
    private val producerProviderConfigs =
        generateProviderConfig("${ApplicationEnv.envPrefix}PROVIDER_MAPPER_PRODUCER_")
    private val consumerProviderConfigs =
        generateProviderConfig("${ApplicationEnv.envPrefix}PROVIDER_MAPPER_CONSUMER_")

    private fun generateProviderConfig(envPrefix: String): List<ProviderConfig> {
        val topicsOfProviders =
            ApplicationEnv.get("${envPrefix}TOPIC_ENVS")?.split(",")?.map { topicEnv ->
                val config =
                    ApplicationEnv.get(topicEnv)
                        ?: throw ConfigException(
                            "ProviderMapper - cannot read $topicEnv while parsing TOPIC_ENVS"
                        )
                config.split(",").toSet()
            }
                ?: return emptyList()
        val providers =
            ApplicationEnv.get("${envPrefix}PROVIDERS")?.split(",")?.map {
                try {
                    Provider.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    throw ConfigException("ProviderMapper - provider $it not found")
                }
            }
                ?: return emptyList()
        val configs =
            ApplicationEnv.get("${envPrefix}CONFIG_ENVS")?.split(",")?.map { configEnv ->
                val config =
                    ApplicationEnv.get(configEnv)
                        ?: throw ConfigException(
                            "ProviderMapper - cannot read $configEnv while parsing CONFIG_ENVS"
                        )
                Json.decodeFromString<JsonObject>(config).toMapAny()
            }
                ?: return emptyList()
        if (topicsOfProviders.size != providers.size || providers.size != configs.size) {
            throw ConfigException(
                "ProviderMapper - sizes are variable: TOP${
                    topicsOfProviders.size}, PR${providers.size}, CONF${configs.size}"
            )
        }

        return List(topicsOfProviders.size) { i ->
            val topics = topicsOfProviders[i]
            val config = configs[i]
            val provider = providers[i]
            ProviderConfig(provider, config, topics)
        }
    }

    private fun getProviderConfig(
        topics: List<String>,
        providerConfigs: List<ProviderConfig>
    ): ProviderConfig? {
        return providerConfigs.firstOrNull { topics.intersect(it.topics).size == topics.size }
    }

    fun getProducerProviderConfig(topics: List<String>) =
        getProviderConfig(topics, producerProviderConfigs)
    fun getConsumerProviderConfig(topics: List<String>) =
        getProviderConfig(topics, consumerProviderConfigs)
}
