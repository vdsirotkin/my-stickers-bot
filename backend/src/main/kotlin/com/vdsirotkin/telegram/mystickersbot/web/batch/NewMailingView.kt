package com.vdsirotkin.telegram.mystickersbot.web.batch

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.textfield.TextArea
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.Route
import com.vdsirotkin.telegram.mystickersbot.batch.JobManager
import com.vdsirotkin.telegram.mystickersbot.batch.JobProcessor
import com.vdsirotkin.telegram.mystickersbot.web.MainLayout
import kotlinx.coroutines.runBlocking
import ru.sokomishalov.commons.core.common.unit
import ru.sokomishalov.commons.core.log.Loggable

@Route(value = "mailing/new", layout = MainLayout::class)
class NewMailingView(
        private val jobManager: JobManager,
        private val jobProcessor: JobProcessor
) : KComposite() {

    private lateinit var name: TextField
    private lateinit var text: TextArea
    private lateinit var textRu: TextArea
    private val binder: Binder<SendBatchMessageRequest> = Binder(SendBatchMessageRequest::class.java)

    private val root = ui {
        binder.bean = SendBatchMessageRequest()
        verticalLayout {
            h1("New mailing")
            name = textField("Name")
            text = textArea("Message (English)")
            textRu = textArea("Message (Russian)")
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
            binder.forMemberField(textRu).asRequired().bind(SendBatchMessageRequest::textRu)
        }
    }

    private fun handleBatchSend() = runBlocking {
        val request = binder.bean
        kotlin.runCatching {
            val job = jobManager.createNewJob(request.name, request.text, request.textRu)
            jobProcessor.startJob(job.id)
            UI.getCurrent().navigate(ShowMailingView::class.java, job.id)
        }.onFailure {
            logError("Can't start batch job, ${it.message}", it)
            Notification.show("Error")
        }
    }.unit()

    data class SendBatchMessageRequest(var text: String = "", var name: String = "", var textRu: String = "")

    companion object : Loggable

}
