package com.vdsirotkin.telegram.mystickersbot.admin

import com.github.fonimus.ssh.shell.SimpleTable
import com.github.fonimus.ssh.shell.SshShellHelper
import com.github.fonimus.ssh.shell.commands.SshShellComponent
import com.vdsirotkin.telegram.mystickersbot.batch.JobManager
import com.vdsirotkin.telegram.mystickersbot.batch.JobProcessor
import kotlinx.coroutines.runBlocking
import org.springframework.shell.standard.ShellMethod
import java.nio.file.Paths

@SshShellComponent
class BatchingAdminService(
    private val jobManager: JobManager,
    private val jobProcessor: JobProcessor,
    private val helper: SshShellHelper,
) {

    @ShellMethod
    fun listBatches() {
        val list = runBlocking { jobManager.listJobs() }
        if (list.isEmpty()) {
            helper.print("Nothing to display :(")
            return
        }
        helper.renderTable(SimpleTable.builder()
            .column("ID")
            .column("Name")
            .lines(list.map { listOf(it.first, it.second) })
            .build()
        ).also { helper.print(it) }
    }

    @ShellMethod
    fun newBatch(textRu: String, textEn: String) = runBlocking {
        val name = helper.read("Please input name:")
        val textRuText = Paths.get(textRu).toFile().readText()
        val textEnText = Paths.get(textEn).toFile().readText()
        helper.print("""Resulting batch:
            |Name: $name
            |Text (russian): $textRuText
            |Text (english): $textEnText
        """.trimMargin())
        val result = helper.confirm("Confirm sending")
        if (!result) {
            helper.print("Aborted")
            return@runBlocking
        }
        jobManager.createNewJob(name, textEnText, textRuText).also {
            jobProcessor.startJob(it.id)
            helper.print("Started job with id: ${it.id}")
        }
    }

    @ShellMethod
    fun jobStatus(jobId: String) = runBlocking {
        val job = jobManager.getJobMeta(jobId)
        helper.print("""Job:
            |ID: ${job.jobMeta.id}
            |Name: ${job.jobMeta.name}
            |Text (russian): ${job.jobMeta.textRu}
            |Text (english): ${job.jobMeta.text}
            |Processed: ${job.jobStats.processedCount} from ${job.jobStats.overallCount}
            |Status: ${job.jobStats.processedCount - job.jobStats.successCount} errors, ${job.jobStats.successCount} successes
        """.trimMargin())
    }
}
