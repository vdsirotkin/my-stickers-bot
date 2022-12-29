@file:Suppress("MemberVisibilityCanBePrivate")

package com.vdsirotkin.telegram.mystickersbot.handler.sticker

import com.pengrad.telegrambot.Callback
import com.pengrad.telegrambot.model.*
import com.pengrad.telegrambot.request.AddStickerToSet
import com.pengrad.telegrambot.request.CreateNewStickerSet
import com.pengrad.telegrambot.request.GetFile
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.response.BaseResponse
import com.pengrad.telegrambot.response.GetFileResponse
import com.pengrad.telegrambot.response.SendResponse
import com.vdsirotkin.telegram.mystickersbot.bot.BotConfigProps
import com.vdsirotkin.telegram.mystickersbot.bot.MyStickersBot
import com.vdsirotkin.telegram.mystickersbot.db.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.db.entity.UserEntity
import com.vdsirotkin.telegram.mystickersbot.fillMdc
import com.vdsirotkin.telegram.mystickersbot.service.FileHelper
import com.vdsirotkin.telegram.mystickersbot.service.StickerPackManagementService
import com.vdsirotkin.telegram.mystickersbot.service.StickerPackMessagesSender
import com.vdsirotkin.telegram.mystickersbot.service.image.PngService
import com.vdsirotkin.telegram.mystickersbot.util.TEMP_FILE_PREFIX
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.platform.commons.util.ReflectionUtils
import org.springframework.context.MessageSource
import ru.sokomishalov.commons.core.common.unit
import ru.sokomishalov.commons.core.reactor.awaitStrict
import java.nio.file.Files

const val CHAT_ID = 123L
const val MESSAGE_ID = 123321

class NormalStickerHandlerTest {

    val normalStickerHandler: NormalStickerHandler
    val defaultState = NormalStickerHandler.NormalStickerHandlerState(NormalStickerHandler.State.New)
    val bot = spyk(mockkClass(MyStickersBot::class))
    val dao: StickerDAO = mockkClass(StickerDAO::class, relaxed = true)

    init {
        val pngSticker = mockkClass(PngService::class) {
            every { webpToPng(any()) } returns Files.createTempFile(TEMP_FILE_PREFIX, "").toFile().also { it.deleteOnExit() }
        }
        val spms = StickerPackManagementService(BotConfigProps(false, "", "", null, 1234L))
        val spmSender = StickerPackMessagesSender()
        val ms = mockkClass(MessageSource::class) {
            every { getMessage(any(), null, any()) } returns ""
        }
        val fileHelper = mockk<FileHelper>().apply {
            coEvery { downloadFile(any(), any(), any()) } returns Unit
        }
        normalStickerHandler = NormalStickerHandler(pngSticker, spms, spmSender, fileHelper, dao, ms)

        every { bot.retry } returns Retry.of("", RetryConfig.custom<Any>().retryOnException { false }.build())
        every { bot.rateLimiter } returns RateLimiter.ofDefaults("")
        every {
            bot.execute(any(), any<Callback<SendMessage, SendResponse>>())
        } answers {
            val clazz = firstArg<Any>()
            when (clazz) {
                is SendMessage -> (secondArg() as Callback<SendMessage, SendResponse>).onResponse(null, ReflectionUtils.newInstance(SendResponse::class.java))
                is GetFile -> {
                    (secondArg() as Callback<GetFile, GetFileResponse>).onResponse(null, mockk<GetFileResponse>().apply {
                        every { file() } returns mockk<File>().apply {
                            every { fileId() } returns "792632f6-230b-4b1b-8b5d-087ffd785482"
                        }
                        every { isOk } returns true
                    })
                }
                is CreateNewStickerSet -> (secondArg() as Callback<CreateNewStickerSet, BaseResponse>).onResponse(null, ReflectionUtils.newInstance(BaseResponse::class.java))
                is AddStickerToSet -> (secondArg() as Callback<AddStickerToSet, BaseResponse>).onResponse(null, ReflectionUtils.newInstance(BaseResponse::class.java))
            }
        }
    }

    @BeforeEach
    fun resetState() {
        normalStickerHandler.setState(defaultState)
        setupDao()
    }

    @Test
    @DisplayName("test scenario with normal sticker")
    fun testScenarioWithNormalSticker() = runBlocking {
        val update = buildStickerUpdate()
        normalStickerHandler.handle(bot, update).fillMdc(CHAT_ID).awaitStrict()

        verify(exactly = 3) { bot.execute(any<CreateNewStickerSet>(), any()) }
    }.unit()

    @Test
    @DisplayName("test scenario with normal sticker and existing pack")
    fun testScenarioWithNormalStickerAndExistingPack() = runBlocking {
        setupDao(true)

        val update = buildStickerUpdate()
        normalStickerHandler.handle(bot, update).fillMdc(CHAT_ID).awaitStrict()

        verify(exactly = 3) { bot.execute(any<SendMessage>(), any()) }
    }.unit()

    @Test
    @DisplayName("test scenario with existing sticker")
    fun testScenarioWithExistingSticker() = runBlocking {
        setupDao(stickerExists = true)

        val update = buildStickerUpdate()
        normalStickerHandler.handle(bot, update).fillMdc(CHAT_ID).awaitStrict()

        verify(exactly = 1) { bot.execute(any<SendMessage>(), any()) }
    }.unit()

    @Test
    @DisplayName("test scenario with no-emoji sticker")
    fun testScenarioWithNoEmojiSticker() = runBlocking {
        val update = buildStickerUpdate(null)
        val firstResult = normalStickerHandler.handle(bot, update).fillMdc(CHAT_ID).awaitStrict()

        val update2 = buildTextUpdate()
        firstResult.handle(bot, update2).fillMdc(CHAT_ID).awaitStrict()

        verify(exactly = 4) { bot.execute(any<SendMessage>(), any()) }
    }.unit()

    @Test
    @DisplayName("test scenario with no-emoji sticker and broken text")
    fun testScenarioWithNoEmojiStickerAndBrokenText() = runBlocking {
        val update = buildStickerUpdate(null)
        normalStickerHandler.handle(bot, update).fillMdc(CHAT_ID).awaitStrict()

        val update2 = buildTextUpdate("asdfasdf")
        normalStickerHandler.handle(bot, update2).fillMdc(CHAT_ID).awaitStrict()
        val update3 = buildTextUpdate()
        normalStickerHandler.handle(bot, update3).fillMdc(CHAT_ID).awaitStrict()

        verify(exactly = 5) { bot.execute(any<SendMessage>(), any()) }
    }.unit()

    private fun buildTextUpdate(text: String? = "😀"): Update {
        return mockkClass(Update::class) upd@{
            val message = mockkClass(Message::class) msg@{
                every { this@msg.text() } returns text
                every { this@msg.chat() } returns mockkClass(Chat::class) cht@{
                    every {this@cht.id()} returns CHAT_ID
                }
                every { this@msg.messageId() } returns MESSAGE_ID
            }
            every { this@upd.message() } returns message
        }
    }

    private fun buildStickerUpdate(emoji: String? = "😀"): Update {
        return mockkClass(Update::class) upd@{
            val message = mockkClass(Message::class) msg@{
                val sticker = mockkClass(Sticker::class) stc@{
                    every { this@stc.emoji() } returns emoji
                    every { this@stc.fileId() } returns ""
                    every { this@stc.fileUniqueId() } returns ""
                    every { this@stc.isVideo } returns false
                    every { this@stc.isAnimated } returns false
                }
                every { this@msg.sticker() } returns sticker
                every { this@msg.chat() } returns mockkClass(Chat::class) cht@{
                    every {this@cht.id()} returns CHAT_ID
                }
                every { this@msg.messageId() } returns MESSAGE_ID
            }
            every { this@upd.message() } returns message
        }
    }

    private fun setupDao(normalPackCreated: Boolean = false, stickerExists: Boolean = false) {
        val entity = buildDefaultEntity(normalPackCreated)
        coEvery { dao.stickerExists(entity, any()) } returns stickerExists
        coEvery { dao.getUserEntity(CHAT_ID) } returns entity
    }

    private fun buildDefaultEntity(normalPackCreated: Boolean = false): UserEntity = UserEntity(CHAT_ID.toString(), "test", "test", "test", normalPackCreated, false)

}
