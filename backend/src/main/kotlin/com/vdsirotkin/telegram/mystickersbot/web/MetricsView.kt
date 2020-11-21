package com.vdsirotkin.telegram.mystickersbot.web

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.FlexLayout
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vdsirotkin.telegram.mystickersbot.db.StickerDAO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking

@Route(value = "metrics", layout = MainLayout::class)
@PageTitle("Metrics")
class MetricsView(
        private val stickerDAO: StickerDAO
): KComposite() {

    private val root = ui {
        val metrics = loadMetrics()
        verticalLayout {
            h1("Metrics")
            flexLayout {
                justifyContentMode = FlexComponent.JustifyContentMode.AROUND
                flexWrap = FlexLayout.FlexWrap.WRAP
                add(Gauge("People", metrics.peopleCount.toString()))
                add(Gauge("Normal stickers", metrics.normal.toString()))
                add(Gauge("Animated sticers", metrics.animated.toString()))
            }
        }
    }

    data class MetricsInfo(
            val peopleCount: Int,
            val normal: Int,
            val animated: Int
    )

    class Gauge(
            private val name: String,
            private val value: String
    ) : KComposite() {
        private val root = ui {
            div {
                style.set("padding", "10px")
                style.set("margin", "15px")
                style.set("box-shadow", "0 0 10px 0 rgba(0, 0, 0, 0.05)")
                style.set("border-radius", "4px")
                verticalLayout {
                    alignItems = FlexComponent.Alignment.CENTER
                    span(value) {
                        style.set("font-size", "30pt")
                        style.set("color", "blue")
                    }
                    span(name) {
                        style.set("font-size", "18pt")
                    }
                }
            }
        }
    }

    private fun loadMetrics(): MetricsInfo = runBlocking {
        val getUsers = async { stickerDAO.countUsers() }
        val getNormalCount = async { stickerDAO.countNormalAddedStickers().toInt() }
        val getAnimatedCount = async { stickerDAO.countAnimatedAddedStickers().toInt() }
        val (users, normal, animated) = listOf(getUsers, getNormalCount, getAnimatedCount).awaitAll()
        MetricsInfo(users, normal, animated)
    }

}