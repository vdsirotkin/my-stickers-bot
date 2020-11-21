package com.vdsirotkin.telegram.mystickersbot.web

import com.github.mvysny.karibudsl.v10.tab
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.applayout.AppLayout
import com.vaadin.flow.component.applayout.DrawerToggle
import com.vaadin.flow.component.dependency.CssImport
import com.vaadin.flow.component.page.Push
import com.vaadin.flow.component.tabs.Tab
import com.vaadin.flow.component.tabs.Tabs
import com.vdsirotkin.telegram.mystickersbot.web.batch.ListMailingView

@CssImport("./" +
        "styles/custom.css")
@Push
class MainLayout : AppLayout() {

    private val map: MutableMap<Tab, () -> Unit> = mutableMapOf()

    init {
        primarySection = Section.DRAWER
        addToNavbar(true, DrawerToggle())
        addToDrawer(Tabs().apply {
            tab("Search") {
                map[this] = { UI.getCurrent().navigate(SearchUserView::class.java) }
            }
            tab("Send") {
                map[this] = { UI.getCurrent().navigate(SendMessageView::class.java) }
            }
            tab("Mailing") {
                map[this] = { UI.getCurrent().navigate(ListMailingView::class.java) }
            }
            tab("Metrics") {
                map[this] = { UI.getCurrent().navigate(MetricsView::class.java) }
            }
            addSelectedChangeListener {
                map[it.selectedTab]!!()
            }
            orientation = Tabs.Orientation.VERTICAL
        })
    }
}
