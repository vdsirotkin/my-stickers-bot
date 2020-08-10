package com.vdsirotkin.telegram.mystickersbot.web.batch

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.textfield.TextArea
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.Route
import com.vdsirotkin.telegram.mystickersbot.batch.JobManager
import com.vdsirotkin.telegram.mystickersbot.batch.JobProcessor
import com.vdsirotkin.telegram.mystickersbot.web.MainLayout
import kotlinx.coroutines.runBlocking
import ru.sokomishalov.commons.core.log.Loggable

@Route(value = "mailing/new", layout = MainLayout::class)
class NewMailingView(
        private val jobManager: JobManager,
        private val jobProcessor: JobProcessor
) : KComposite() {

    private lateinit var name: TextField
    private lateinit var text: TextArea
    private val binder: Binder<SendBatchMessageRequest> = Binder(SendBatchMessageRequest::class.java)

    private val root = ui {
        binder.bean = SendBatchMessageRequest()
        horizontalLayout {
            verticalLayout {
                name = textField("Name")
                text = textArea("Message")
                horizontalLayout {
                    content { align(between, top) }
                    button("Send") {
                        onLeftClick { handleBatchSend() }
                        setPrimary()
                    }
                    button("Back") {
                        onLeftClick { ui.ifPresent { it.navigate(ListMailingView::class.java) } }
                    }
                }
                binder.forMemberField(name).asRequired().bind(SendBatchMessageRequest::name)
                binder.forMemberField(text).asRequired().bind(SendBatchMessageRequest::text)
            }
        }
    }

    private fun handleBatchSend() = runBlocking {
        val request = binder.bean
        kotlin.runCatching {
            val job = jobManager.createNewJob(request.name, request.text)
            jobProcessor.startJob(job.id)
        }.fold({
            Notification.show("Successfully started")
        }, {
            logError("Can't start batch job, ${it.message}", it)
            Notification.show("Error")
        })
    }

    data class SendBatchMessageRequest(var text: String = "", var name: String = "")

    companion object : Loggable

}
