package com.j0rsa.labot

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.extensions.filters.Filter
import com.j0rsa.labot.client.Anki
import com.j0rsa.labot.client.Chatterbug
import com.j0rsa.labot.client.Skyeng
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object Main {
    private val stateMap: MutableMap<Long, State> = mutableMapOf()

    private fun setState(userId: Long, state: State) {
        stateMap[userId] = state
    }

    private fun getState(userId: Long): State = stateMap[userId] ?: State.None

    private val skyeng = Skyeng(
        AppConfig.config.skyeng.user,
        AppConfig.config.skyeng.password,
    )
    private var lastSkyengUpdate = AppConfig.config.skyeng.uploadAfter
        ?: LocalDate.now().minusWeeks(1).format(DateTimeFormatter.ISO_LOCAL_DATE)

    private val ankiTvConfig = AppConfig.config.anki["tv"]!!
    private val ankiTv = Anki(ankiTvConfig.url)
    private val ankiKConfig = AppConfig.config.anki["k"]!!
    private val ankiK = Anki(ankiKConfig.url)

    @JvmStatic
    fun main(args: Array<String>) {
        println("Starting bot")
        bot {
            token = AppConfig.config.telegram.token
            dispatch {
                callbackQuery {
                    when (callbackQuery.data.split(":").firstOrNull()) {
                        State.SkyengSync.name -> {
                            val date = LocalDate.parse(callbackQuery.data.split(":")[1])
                            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
                            runBlocking {
                                skyengSyncPerform(bot, ChatId.fromId(chatId), date)
                            }
                        }

                        else -> {}
                    }
                }
                command("start") {
                    bot.sendMessage(
                        ChatId.fromId(message.chat.id),
                        text = "Hey, ${message.from?.username ?: "user"}(${message.from?.id})!"
                    )
                }
                command("skyeng_sync") {
                    val userId = message.from?.id ?: return@command
                    setState(userId, State.SkyengSync)
                    bot.sendMessage(
                        ChatId.fromId(message.chat.id),
                        text = "Enter a date in format yyyy-mm-dd or tab the button below to use the last known",
                        replyMarkup = InlineKeyboardMarkup.create(
                            listOf(
                                InlineKeyboardButton.CallbackData(
                                    lastSkyengUpdate,
                                    "${State.SkyengSync}:$lastSkyengUpdate"
                                )
                            )
                        )
                    )
                }
                command("chatterbug_export") {
                    val userId = message.from?.id ?: return@command
                    setState(userId, State.ChatterbugExport)
                    bot.sendMessage(ChatId.fromId(message.chat.id), "Which unit id do you want to export?")
                }

                message(Filter.Text and Filter.Command.not()) {
                    if (message.from?.id !in AppConfig.config.telegram.allowedUsers) return@message
                    val userId = message.from?.id ?: return@message
                    val text = message.text ?: return@message
                    when (getState(userId)) {
                        State.SkyengSync -> {
                            try {
                                val updateAfter: LocalDate = LocalDate.parse(text)
                                runBlocking {
                                    skyengSyncPerform(bot, ChatId.fromId(message.chat.id), updateAfter)
                                }
                                setState(userId, State.None)
                            } catch (e: DateTimeParseException) {
                                bot.sendMessage(
                                    ChatId.fromId(message.chat.id),
                                    "Enter a date in format yyyy-mm-dd or tab the button below to use the last known",
                                    replyMarkup = InlineKeyboardMarkup.create(
                                        listOf(
                                            InlineKeyboardButton.CallbackData(
                                                lastSkyengUpdate,
                                                "${State.SkyengSync}:$lastSkyengUpdate"
                                            )
                                        )
                                    )
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                                bot.sendMessage(
                                    ChatId.fromId(message.chat.id),
                                    text = "An error occurred: ${e.localizedMessage}"
                                )
                            }
                        }

                        State.ChatterbugExport -> {
                            text.toLongOrNull()?.let { id ->
                                runBlocking {
                                    chatterbugExportPerform(bot, ChatId.fromId(message.chat.id), id)
                                }
                                setState(userId, State.None)
                            } ?: bot.sendMessage(ChatId.fromId(message.chat.id), "Which unit id do you want to export?")
                        }

                        else -> bot.sendMessage(ChatId.fromId(message.chat.id), text = "Idk what are you talking about")
                    }
                }
            }
        }.startPolling()
    }

    private suspend fun skyengSyncPerform(bot: Bot, chatId: ChatId, updateAfter: LocalDate) {
        val token = skyeng.login()
        token.ifEmpty {
            throw IllegalStateException("Unable to login! Please check the credentials!")
        }
        val words = skyeng.getWords(token, AppConfig.config.skyeng.studentId).map { it.word }
            .filter { it.createdAt.toLocalDate() > updateAfter }
        bot.sendMessage(chatId, "Fetched words: ${words.size}")
        val meanings = skyeng.getMeaning(token, words)
        bot.sendMessage(chatId, "Fetched meanings: ${meanings.size}")
        val notes = meanings.flatMap { it.toAnkiClozeNote("skyeng") }.filterNotNull()
        bot.sendMessage(chatId, "Detected notes: ${notes.size}")
        bot.sendMessage(chatId, "Uploading notes")
        ankiTv.addNotes(notes)
        bot.sendMessage(chatId, "Syncing")
        ankiTv.sync()
        bot.sendMessage(chatId, "Done!")
        lastSkyengUpdate = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
    }

    private suspend fun chatterbugExportPerform(bot: Bot, chatId: ChatId, unitId: Long) {
        val config = AppConfig.config.chatterbug
        val chatterbug = Chatterbug(config.user, config.password)
        val results = chatterbug.login().getWordsFromUnit(unitId.toString())
        bot.sendMessage(chatId, "Found ${results?.curriculum?.unit?.words?.size} word(s)")
        val notes = Chatterbug.toAnkiClozeNote(results, "test")
        bot.sendMessage(chatId, "Composed ${notes.size} notes")
        bot.sendMessage(chatId, "Uploading notes")
        ankiK.addNotes(notes)
        bot.sendMessage(chatId, "Syncing")
        ankiK.sync()
        bot.sendMessage(chatId, "Done!")
    }
}

enum class State {
    SkyengSync,
    ChatterbugExport,
    None
}
