package com.vdsirotkin.telegram.mystickersbot.web

import com.github.mvysny.karibudsl.v10.*
import com.pengrad.telegrambot.request.GetChat
import com.vaadin.flow.component.Key
import com.vaadin.flow.component.html.Article
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.Route
import com.vdsirotkin.telegram.mystickersbot.bot.MyStickersBot
import ru.sokomishalov.commons.core.log.Loggable

const val SEARCH_USER = "search/user"

@Route(value = SEARCH_USER, layout = MainLayout::class)
class SearchUserView(
        private val bot: MyStickersBot
) : KComposite() {

    private lateinit var currentUserInfo: Article
    private lateinit var userId: TextField
    private val binder: Binder<SearchUserInfo> = Binder(SearchUserInfo::class.java)

    private val root = ui {
        binder.bean = SearchUserInfo()
        horizontalLayout {
            verticalLayout {
                userId = textField("User id")
                val button = button("Submit").apply {
                    addClickShortcut(Key.ENTER)
                    onLeftClick {
                        handleSubmit(this@verticalLayout)
                    }
                }
                currentUserInfo = article {}
                binder.forMemberField(userId).validateNotBlank().asRequired().bind(SearchUserInfo::userId)
                binder.addStatusChangeListener {
                    button.isEnabled = !it.hasValidationErrors()
                }
            }
        }
    }

    private fun handleSubmit(layout: VerticalLayout) {
        if (binder.validate().hasErrors()) return

        val bean = binder.bean
        val result = runCatching {
            bot.execute(GetChat(bean.userId))
        }
        if (result.isSuccess) {
            val chat = result.getOrNull()
            val newInfo = Article().apply {
                span("Username: ${chat?.chat()?.username()}")
                br()
                span("First name: ${chat?.chat()?.firstName()}")
                br()
                span("Last name: ${chat?.chat()?.lastName()}")
            }
            layout.replace(currentUserInfo, newInfo)
            currentUserInfo = newInfo
        } else {
            logError(result.exceptionOrNull()?.message, result.exceptionOrNull()!!)
            val newInfo = Article().apply {
                span("Some error occurred")
            }
            layout.replace(currentUserInfo, newInfo)
            currentUserInfo = newInfo
        }
    }

    data class SearchUserInfo(
            var userId: String? = null
    )

    companion object : Loggable

}
