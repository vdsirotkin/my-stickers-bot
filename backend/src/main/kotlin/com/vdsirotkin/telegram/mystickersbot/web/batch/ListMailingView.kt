package com.vdsirotkin.telegram.mystickersbot.web.batch

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.UI
import com.vaadin.flow.router.Route
import com.vdsirotkin.telegram.mystickersbot.batch.JobManager
import com.vdsirotkin.telegram.mystickersbot.web.MainLayout
import kotlinx.coroutines.runBlocking

@Route(value = "mailing/list", layout = MainLayout::class)
class ListMailingView(
        private val jobManager: JobManager
): KComposite() {

    private val root = ui {
        horizontalLayout {
            val list = runBlocking { jobManager.listJobs() }
            if (list.isEmpty()) {
                div("info-panel") {
                    text = "Nothing found :("
                }
            } else {
                ul {
                    list.forEach { pair ->
                        li {
                            button(text = pair.second).apply {
                                onLeftClick { showMailing(pair.first) }
                            }
                        }
                    }
                }
            }
            button("New") {
                onLeftClick { ui.ifPresent { it.navigate(NewMailingView::class.java) } }
                setPrimary()
            }
        }
    }

    private fun showMailing(id: String) {
        UI.getCurrent().navigate(ShowMailingView::class.java, id)
    }

}
