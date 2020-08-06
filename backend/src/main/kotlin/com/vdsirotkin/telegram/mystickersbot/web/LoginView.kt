package com.vdsirotkin.telegram.mystickersbot.web

import com.github.mvysny.karibudsl.v10.KComposite
import com.github.mvysny.karibudsl.v10.content
import com.github.mvysny.karibudsl.v10.loginForm
import com.github.mvysny.karibudsl.v10.verticalLayout
import com.vaadin.flow.component.Tag
import com.vaadin.flow.component.login.LoginForm
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route

@Tag("sa-login-view")
@Route(value = LoginView.ROUTE)
@PageTitle("Login")
class LoginView : KComposite(), BeforeEnterObserver {
    private lateinit var login: LoginForm
    override fun beforeEnter(event: BeforeEnterEvent) {
        if (event.location.queryParameters.parameters.getOrDefault("error", emptyList()).isNotEmpty()) {
            login.isError = true
        }
    }

    private val root = ui {
        verticalLayout {
            content { align(center, top) }
            login = loginForm {
                action = "login"
            }
        }
    }

    companion object {
        const val ROUTE = "login"
    }
}
