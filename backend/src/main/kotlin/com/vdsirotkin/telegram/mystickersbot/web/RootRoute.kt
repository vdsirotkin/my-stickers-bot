package com.vdsirotkin.telegram.mystickersbot.web

import com.vaadin.flow.component.AttachEvent
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.router.Route

@Route("")
class RootRoute : Div() {

    override fun onAttach(attachEvent: AttachEvent?) {
        UI.getCurrent().navigate(SearchUserView::class.java)
    }
}
