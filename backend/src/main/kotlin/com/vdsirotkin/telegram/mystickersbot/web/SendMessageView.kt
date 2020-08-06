package com.vdsirotkin.telegram.mystickersbot.web

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.textfield.TextArea
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.Route
import com.vdsirotkin.telegram.mystickersbot.bot.MyStickersBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import ru.sokomishalov.commons.core.log.Loggable

@Route(value = "send/message", layout = MainLayout::class)
class SendMessageView(
        private val bot: MyStickersBot
): KComposite() {

    private lateinit var userId: TextField
    private lateinit var text: TextArea
    private val binder: Binder<SendMessageRequest> = Binder(SendMessageRequest::class.java)

    private val root = ui {
        binder.bean = SendMessageRequest()
        verticalLayout {
            userId = textField("User id")
            text = textArea("Message")
            val button = button("Send") {
                onLeftClick { handleSendMessage() }
            }
            binder.forMemberField(userId).asRequired().bind(SendMessageRequest::userId)
            binder.forMemberField(text).asRequired().bind(SendMessageRequest::text)
            binder.addStatusChangeListener {
                button.isEnabled = !it.hasValidationErrors()
            }
        }
    }

    private fun handleSendMessage() {
        if (binder.validate().hasErrors()) return

        val (userIdValue, textValue) = binder.bean
        try {
            bot.execute(SendMessage(userIdValue, textValue).enableHtml(true))
            Notification.show("Successfully sent")
        } catch (e: Exception) {
            logError("Can't send to user: ${e.message}", e)
            Notification.show("Error")
        }
    }

    data class SendMessageRequest(var userId: String, var text: String) {
        constructor() : this("", "")
    }

    companion object : Loggable

}
