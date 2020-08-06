package com.vdsirotkin.telegram.mystickersbot.web.batch

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.HasText
import com.vaadin.flow.component.HasValue
import com.vaadin.flow.component.html.Article
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.data.binder.ReadOnlyHasValue
import com.vaadin.flow.router.BeforeEvent
import com.vaadin.flow.router.HasUrlParameter
import com.vaadin.flow.router.Route
import com.vdsirotkin.telegram.mystickersbot.batch.JobManager
import com.vdsirotkin.telegram.mystickersbot.web.MainLayout
import kotlinx.coroutines.runBlocking
import ru.sokomishalov.commons.core.common.unit

@Route(value = "mailing/show", layout = MainLayout::class)
class ShowMailingView(
        private val jobManager: JobManager
) : KComposite(), HasUrlParameter<String> {

    private lateinit var title: H2
    private lateinit var article: Article
    private val binder: Binder<BatchViewInfo> = Binder(BatchViewInfo::class.java)

    private val root = ui {
        verticalLayout {
            button("Back").apply {
                onLeftClick { ui.ifPresent { it.navigate(ListMailingView::class.java) } }
            }
            title = h2().also { binder.forField(it.toHasValue()).bind(BatchViewInfo::name) }
            article = article {
                span("ID: ") {
                    span().also { binder.forField(it.toHasValue()).bind(BatchViewInfo::id) }
                }
                br()
                span("Text: ") {
                    span().also { binder.forField(it.toHasValue()).bind(BatchViewInfo::text) }
                }
            }
        }
    }

    override fun setParameter(event: BeforeEvent, parameter: String) = runBlocking {
        val meta = jobManager.getJobMeta(parameter)
        val jobMeta = meta.jobMeta
        val (id1, name, text, _, _) = jobMeta
        val bean = BatchViewInfo(id1, name, text, meta.jobStats.processedCount, meta.jobStats.overallCount, meta.jobStats.successCount, meta.jobStats.overallCount - meta.jobStats.successCount)
        binder.readBean(bean)
    }.unit()

    data class BatchViewInfo(var id: String, var name: String, var text: String, var processedCount: Long,
                             var overallCount: Long, var successCount: Long, var errorCount: Long)

    private fun HasText.toHasValue(): HasValue<HasValue.ValueChangeEvent<String>, String> {
        return ReadOnlyHasValue(this::setText)
    }

}
