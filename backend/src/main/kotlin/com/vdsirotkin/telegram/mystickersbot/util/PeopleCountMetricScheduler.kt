package com.vdsirotkin.telegram.mystickersbot.util

import com.vdsirotkin.telegram.mystickersbot.dao.StickerDAO
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.runBlocking
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import ru.sokomishalov.commons.core.log.Loggable
import java.util.concurrent.atomic.AtomicInteger
import javax.annotation.PostConstruct

@Service
class PeopleCountMetricScheduler(
        private val registry: MeterRegistry,
        private val stickerDAO: StickerDAO
) {

    val count = AtomicInteger(0)

    @PostConstruct
    fun init() {
        registry.gauge("people.count", count) { count.get().toDouble() }
    }

    @Scheduled(fixedDelay = 10*60*1000, initialDelay = 0)
    fun updateCounter() = runBlocking {
        val users = stickerDAO.countUsers()
        count.set(users)
        logger.info("Updated users count, now it is $users")
    }

    companion object : Loggable

}
