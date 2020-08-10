package com.vdsirotkin.telegram.mystickersbot.web.batch

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.DetachEvent
import com.vaadin.flow.component.HasText
import com.vaadin.flow.component.HasValue
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.data.binder.ReadOnlyHasValue
import com.vaadin.flow.router.BeforeEvent
import com.vaadin.flow.router.HasUrlParameter
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.VaadinService
import com.vdsirotkin.telegram.mystickersbot.batch.JobManager
import com.vdsirotkin.telegram.mystickersbot.web.MainLayout
import kotlinx.coroutines.*
import ru.sokomishalov.commons.core.common.unit
import ru.sokomishalov.commons.core.log.Loggable

@Route(value = "mailing/show", layout = MainLayout::class)
class ShowMailingView(
        private val jobManager: JobManager
) : KComposite(), HasUrlParameter<String> {

    private val binder: Binder<BatchViewInfo> = Binder(BatchViewInfo::class.java)
    private var bean: BatchViewInfo? = null
    private var job: Job? = null

    private val root = ui {
        startAsyncPush()
        horizontalLayout {
            verticalLayout {
                button("Back").apply {
                    onLeftClick { ui.ifPresent { it.navigate(ListMailingView::class.java) } }
                }
                h2().also { binder.forField(it.toHasValue()).bind(BatchViewInfo::name) }
                article {
                    span("ID: ") {
                        span().also { binder.forField(it.toHasValue()).bind(BatchViewInfo::id) }
                    }
                    br()
                    span("Text: ") {
                        span().also { binder.forField(it.toHasValue()).bind(BatchViewInfo::text) }
                    }
                    br()
                    span("Processed: ") {
                        span().also { binder.forField(it.toHasValue()).bind({ it.processedCount.toString() }, { _, _ -> }) }
                        text(" from ")
                        span().also { binder.forField(it.toHasValue()).bind({ it.overallCount.toString() }, { _, _ -> }) }
                    }
                    br()
                    span("Status: ") {
                        span().also { binder.forField(it.toHasValue()).bind({ it.errorCount.toString() }, { _, _ -> }) }
                        text(" errors, ")
                        span().also { binder.forField(it.toHasValue()).bind({ it.successCount.toString() }, { _, _ -> }) }
                        text(" successes.")
                    }
                }
            }
        }
    }

    override fun onDetach(detachEvent: DetachEvent?) {
        job?.cancel()
    }

    private fun startAsyncPush() {
        val vaadinService = VaadinService.getCurrent()
        job = GlobalScope.launch {
            while (bean == null && this.isActive) {
                delay(1000)
            }
            while (bean!!.processedCount != bean!!.overallCount && this.isActive) {
                if (ui.isPresent && vaadinService.isUIActive(ui.get())) {
                    ui.get().access {
                        runBlocking { loadById(bean!!.id) }
                    }
                } else {
                    this.cancel()
                }
                delay(1000)
            }
        }
    }

    override fun setParameter(event: BeforeEvent, parameter: String) = runBlocking {
        loadById(parameter)
    }.unit()

    private suspend fun loadById(jobId: String) {
        val meta = jobManager.getJobMeta(jobId)
        val jobMeta = meta.jobMeta
        val (id1, name, text, _, _) = jobMeta
        bean = BatchViewInfo(id1, name, text, meta.jobStats.processedCount, meta.jobStats.overallCount, meta.jobStats.successCount, meta.jobStats.overallCount - meta.jobStats.successCount)
        binder.readBean(bean)
//        logger.info("updated status for ${bean!!.id}")
    }

    data class BatchViewInfo(var id: String,
                             var name: String,
                             var text: String,
                             var processedCount: Long,
                             var overallCount: Long,
                             var successCount: Long,
                             var errorCount: Long)

    private fun HasText.toHasValue(): HasValue<HasValue.ValueChangeEvent<String>, String> {
        return ReadOnlyHasValue(this::setText)
    }

    companion object : Loggable

}
