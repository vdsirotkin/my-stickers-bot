package com.vdsirotkin.telegram.mystickersbot.admin

import com.github.fonimus.ssh.shell.SimpleTable
import com.github.fonimus.ssh.shell.SshShellHelper
import com.github.fonimus.ssh.shell.commands.SshShellComponent
import com.vdsirotkin.telegram.mystickersbot.batch.JobManager
import kotlinx.coroutines.runBlocking
import org.springframework.shell.standard.ShellMethod
import java.nio.file.Paths

@SshShellComponent
class BatchingAdminService(
    private val jobManager: JobManager,
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
    fun newBatch(textRu: String, textEn: String) {
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
            return
        }
        helper.print("sending")
    }
}
