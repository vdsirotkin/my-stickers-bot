@file:Suppress("MemberVisibilityCanBePrivate")

package com.vdsirotkin.telegram.mystickersbot.handler.sticker

import com.vdsirotkin.telegram.mystickersbot.bot.BotConfigProps
import com.vdsirotkin.telegram.mystickersbot.bot.MyStickersBot
import com.vdsirotkin.telegram.mystickersbot.db.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.db.entity.UserEntity
import com.vdsirotkin.telegram.mystickersbot.fillMdc
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
import org.springframework.context.MessageSource
import org.telegram.telegrambots.meta.api.methods.GetFile
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.stickers.AddStickerToSet
import org.telegram.telegrambots.meta.api.methods.stickers.CreateNewStickerSet
import org.telegram.telegrambots.meta.api.objects.File
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.stickers.Sticker
import org.telegram.telegrambots.meta.updateshandlers.SentCallback
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
        normalStickerHandler = NormalStickerHandler(pngSticker, spms, spmSender, dao, ms)

        every { bot.retry } returns Retry.of("", RetryConfig.custom<Any>().retryOnException { false }.build())
        every { bot.rateLimiter } returns RateLimiter.ofDefaults("")
        every { bot.executeAsync(any<SendMessage>(), any()) } answers { (secondArg() as SentCallback<Message>).onResult(null, Message()) }
        every { bot.executeAsync(any<GetFile>(), any()) } answers { (secondArg() as SentCallback<File>).onResult(null, File()) }
        every { bot.downloadFile(any<File>(), any()) } returns null
        every { bot.execute(any<CreateNewStickerSet>()) } returns true
        every { bot.execute(any<AddStickerToSet>()) } returns true
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

        verify(exactly = 1) { bot.execute(any<CreateNewStickerSet>()) }
        verify(exactly = 2) { bot.executeAsync(any<SendMessage>(), any()) }
    }.unit()

    @Test
    @DisplayName("test scenario with normal sticker and existing pack")
    fun testScenarioWithNormalStickerAndExistingPack() = runBlocking {
        setupDao(true)

        val update = buildStickerUpdate()
        normalStickerHandler.handle(bot, update).fillMdc(CHAT_ID).awaitStrict()

        verify(exactly = 1) { bot.execute(any<AddStickerToSet>()) }
        verify(exactly = 2) { bot.executeAsync(any<SendMessage>(), any()) }
    }.unit()

    @Test
    @DisplayName("test scenario with existing sticker")
    fun testScenarioWithExistingSticker() = runBlocking {
        setupDao(stickerExists = true)

        val update = buildStickerUpdate()
        normalStickerHandler.handle(bot, update).fillMdc(CHAT_ID).awaitStrict()

        verify(exactly = 1) { bot.executeAsync(any<SendMessage>(), any()) }
    }.unit()

    @Test
    @DisplayName("test scenario with no-emoji sticker")
    fun testScenarioWithNoEmojiSticker() = runBlocking {
        val update = buildStickerUpdate(null)
        val firstResult = normalStickerHandler.handle(bot, update).fillMdc(CHAT_ID).awaitStrict()

        val update2 = buildTextUpdate()
        firstResult.handle(bot, update2).fillMdc(CHAT_ID).awaitStrict()

        verify(exactly = 1) { bot.execute(any<CreateNewStickerSet>()) }
        verify(exactly = 3) { bot.executeAsync(any<SendMessage>(), any()) }
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

        verify(exactly = 1) { bot.execute(any<CreateNewStickerSet>()) }
        verify(exactly = 4) { bot.executeAsync(any<SendMessage>(), any()) }
    }.unit()

    private fun buildTextUpdate(text: String? = "😀"): Update {
        return mockkClass(Update::class) upd@{
            every { this@upd.hasMessage() } returns true
            val message = mockkClass(Message::class) msg@{
                every { this@msg.text } returns text
                every { this@msg.hasText() } returns true
                every { this@msg.chatId } returns CHAT_ID
                every { this@msg.messageId } returns MESSAGE_ID
            }
            every { this@upd.message } returns message
        }
    }

    private fun buildStickerUpdate(emoji: String? = "😀"): Update {
        return mockkClass(Update::class) upd@{
            every { this@upd.hasMessage() } returns true
            val message = mockkClass(Message::class) msg@{
                val sticker = mockkClass(Sticker::class) stc@{
                    every { this@stc.emoji } returns emoji
                    every { this@stc.fileId } returns ""
                    every { this@stc.fileUniqueId } returns ""
                }
                every { this@msg.sticker } returns sticker
                every { this@msg.chatId } returns CHAT_ID
                every { this@msg.messageId } returns MESSAGE_ID
            }
            every { this@upd.message } returns message
        }
    }

    private fun setupDao(normalPackCreated: Boolean = false, stickerExists: Boolean = false) {
        val entity = buildDefaultEntity(normalPackCreated)
        coEvery { dao.stickerExists(entity, any(), false) } returns stickerExists
        coEvery { dao.getUserEntity(CHAT_ID) } returns entity
    }

    private fun buildDefaultEntity(normalPackCreated: Boolean = false): UserEntity = UserEntity(CHAT_ID.toString(), "test", "test", normalPackCreated, false)

}
