package com.vdsirotkin.telegram.mystickersbot.migration

import com.github.fonimus.ssh.shell.SshShellHelper
import com.github.fonimus.ssh.shell.commands.SshShellComponent
import com.vdsirotkin.telegram.mystickersbot.db.entity.UserEntity
import org.springframework.dao.DataAccessException
import org.springframework.shell.standard.ShellMethod
import java.util.concurrent.atomic.AtomicInteger

@SshShellComponent
class MigrationService(
    private val dao: MigrationDAO,
    private val helper: SshShellHelper,
) {

    @ShellMethod
    fun migrateUsers() {
        val processed = AtomicInteger()
        val overallCount = dao.countUsers()
        var updatingList = dao.getUnmigratedBatch(100)
        while (updatingList.isNotEmpty()) {
            updatingList.forEach {
                val result = immediateRetry {
                    if (it.normalPackCreated) {
                        it.stickerSets.add(UserEntity.StaticStickerSet(it.normalPackName, it.normalPackSet))
                    }
                    if (it.animatedPackCreated) {
                        it.stickerSets.add(UserEntity.AnimatedStickerSet(it.animatedPackName, it.animatedPackSet))
                    }
                    if (it.videoPackCreated) {
                        it.stickerSets.add(UserEntity.VideoStickerSet(it.videoPackName, it.videoPackSet))
                    }
                    it.migrated = true
                    dao.updateOne(it).block()
                    processed.incrementAndGet()
                }
                if (result.isFailure) {
                    helper.printError("Can't process entity for ${it.userId}, exception: ${result.exceptionOrNull()?.message}")
                }
            }
            helper.printInfo("Processed ${processed.get()} of $overallCount")
            updatingList = dao.getUnmigratedBatch(100)
        }
    }

    private fun immediateRetry(block: () -> Unit): Result<Unit> {
        var counter = 0
        while (counter < 3) {
            try {
                block()
                return Result.success(Unit)
            } catch (e: DataAccessException) {
                helper.printWarning("Can't process entity due to optimistic lock, trying again. Exception: ${e.message}")
            } catch (e: Exception) {
                return Result.failure(e)
            }
            counter++
        }
        return Result.failure(UnprocessableException())
    }

    class UnprocessableException : Exception("Can't process entity even after optimistic locks")
}
