package com.vdsirotkin.telegram.mystickersbot.web

import com.vdsirotkin.telegram.mystickersbot.batch.JobManager
import com.vdsirotkin.telegram.mystickersbot.batch.JobProcessor
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import ru.sokomishalov.commons.core.log.Loggable

@Controller
class BatchSendController(
        private val jobManager: JobManager,
        private val jobProcessor: JobProcessor
) {

    @GetMapping("/batch/list")
    suspend fun listBatch(model: Model): String {
        val list = jobManager.listJobs()
        model.addAttribute("batchIds", list.map { BatchListInfo(it.first, it.second) })
        return "batch/list"
    }

    @GetMapping("/batch/show")
    suspend fun showBatchJob(@RequestParam("id") id: String, model: Model): String {
        val meta = jobManager.getJobMeta(id)
        model.addAttribute("batch", meta.let {
            val jobMeta = it.jobMeta
            val (id1, name, text, _, _) = jobMeta
            BatchViewInfo(id1, name, text, it.jobStats.processedCount, it.jobStats.overallCount, it.jobStats.successCount, it.jobStats.overallCount - it.jobStats.successCount)
        })
        return "batch/show"
    }

    @GetMapping("/batch/new")
    fun newBatch(model: Model):String {
        model.addAttribute("request", NewBatchRequest())
        return "batch/new"
    }

    @PostMapping("/batch/new")
    suspend fun startNewBatch(@ModelAttribute request: NewBatchRequest, model: Model): String {
        model.addAttribute("request", request)
        return kotlin.runCatching {
            val job = jobManager.createNewJob(request.name, request.text)
            jobProcessor.startJob(job.id)
        }.fold({
            model.addAttribute("result", "success")
            "batch/new"
        }, {
            logError("Can't start batch job, ${it.message}", it)
            model.addAttribute("result", "error")
            "batch/new"
        })
    }

    data class BatchListInfo(val id: String, val name: String)

    data class BatchViewInfo(val id: String, val name: String, val text: String, val processedCount: Long, val overallCount: Long, val successCount: Long, val errorCount: Long)

    data class NewBatchRequest(var name: String = "", var text: String = "")

    companion object : Loggable

}
