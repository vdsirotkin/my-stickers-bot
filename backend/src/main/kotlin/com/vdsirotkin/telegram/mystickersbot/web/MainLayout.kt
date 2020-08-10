package com.vdsirotkin.telegram.mystickersbot.web

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.HasElement
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.dependency.CssImport
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.page.Push
import com.vaadin.flow.component.tabs.Tab
import com.vaadin.flow.router.RouterLayout
import com.vdsirotkin.telegram.mystickersbot.web.batch.ListMailingView

@CssImport("./" +
        "styles/custom.css")
@Push
class MainLayout : KComposite(), RouterLayout {

    private lateinit var layout: VerticalLayout
    private val map: MutableMap<Tab, () -> Unit> = mutableMapOf()

    private val root = ui {
        layout = verticalLayout {
            alignItems = FlexComponent.Alignment.CENTER
            horizontalLayout {
                tabs {
                    tab("Search") {
                        map[this] = { UI.getCurrent().navigate(SearchUserView::class.java) }
                    }
                    tab("Send") {
                        map[this] = { UI.getCurrent().navigate(SendMessageView::class.java) }
                    }
                    tab("Mailing") {
                        map[this] = { UI.getCurrent().navigate(ListMailingView::class.java) }
                    }
                }.addSelectedChangeListener {
                    map[it.selectedTab]!!()
                }
            }
        }
        layout
    }

    override fun showRouterLayoutContent(content: HasElement?) {
        layout.add(content as Component)
    }
}
