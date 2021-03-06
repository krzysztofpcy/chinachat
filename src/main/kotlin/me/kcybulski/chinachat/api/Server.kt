package me.kcybulski.chinachat.api

import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import me.kcybulski.chinachat.domain.ChatFactory
import me.kcybulski.chinachat.domain.ChatsList
import me.kcybulski.chinachat.domain.Security
import me.kcybulski.chinachat.domain.ports.FilesStorage
import ratpack.func.Action
import ratpack.handling.Chain
import ratpack.handling.Context
import ratpack.handling.RequestLogger
import ratpack.server.RatpackServer

class Server(
    private val chats: ChatsList,
    private val chatFactory: ChatFactory,
    private val filesStorage: FilesStorage,
    private val security: Security = Security(),
    private val objectMapper: ObjectMapper = ObjectMapper()
        .registerModule(KotlinModule())
        .setSerializationInclusion(NON_NULL)
) {

    private val ratpackServer: RatpackServer = RatpackServer.of { server ->
        server
            .serverConfig { config ->
                config.threads(1)
            }
            .registryOf { registry ->
                registry
                    .add(objectMapper)
                    .add(security)
            }
            .handlers(api())
    }

    fun start() = ratpackServer.start()
    fun stop() = ratpackServer.stop()

    private fun api(): Action<Chain> = Action { chain ->
        chain
            .all(RequestLogger.ncsa())
            .all { addCORSHeaders(it) }
            .post("login", LoginApi(security))
            .post("upload", ImagesApi(security, filesStorage))
            .prefix("chats") { ChatApi(it, chats, chatFactory) }
    }

    private fun addCORSHeaders(ctx: Context) = ctx
        .header("Access-Control-Allow-Origin", "*")
        .header("Access-Control-Allow-Headers", "*")
        .header("Access-Control-Allow-Methods", "GET, PUT, POST, DELETE")
        .next()

}
