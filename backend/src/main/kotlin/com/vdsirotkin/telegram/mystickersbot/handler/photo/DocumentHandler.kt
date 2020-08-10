package com.vdsirotkin.telegram.mystickersbot.handler.photo

import com.vdsirotkin.telegram.mystickersbot.db.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.handler.BaseHandler
import com.vdsirotkin.telegram.mystickersbot.service.FileHelper
import com.vdsirotkin.telegram.mystickersbot.service.StickerPackManagementService
import com.vdsirotkin.telegram.mystickersbot.service.StickerPackMessagesSender
import com.vdsirotkin.telegram.mystickersbot.service.image.ImageResizer
import com.vdsirotkin.telegram.mystickersbot.util.MessageSourceWrapper
import com.vdsirotkin.telegram.mystickersbot.util.executeAsync
import com.vdsirotkin.telegram.mystickersbot.util.mdcMono
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import reactor.core.publisher.Mono

@Component
class DocumentHandler(override val stickerPackMessagesSender: StickerPackMessagesSender,
                      override val stickerPackManagementService: StickerPackManagementService,
                      override val imageResizer: ImageResizer,
                      override val fileHelper: FileHelper,
                      override val stickerDao: StickerDAO,
                      override val messageSource: MessageSource

) : BasePhotoHandler {


    override fun handleInternal(bot: DefaultAbsSender,
                                update: Update,
                                messageSource: MessageSourceWrapper): Mono<BaseHandler> = mdcMono {
        val chatId = update.message.chatId
        bot.executeAsync(SendMessage(chatId, messageSource.getMessage("please.wait")))
        val document = update.message.document
        when (document.mimeType) {
            "image/jpeg", "image/png" -> {
                val entity = stickerDao.getUserEntity(chatId)
                processPhoto(bot, document.fileId, entity, chatId, update.message.messageId, messageSource)
            }
            else -> bot.executeAsync(SendMessage(chatId, messageSource.getMessage("document.not.supported")))
        }
    }.thenReturn(this)


}
