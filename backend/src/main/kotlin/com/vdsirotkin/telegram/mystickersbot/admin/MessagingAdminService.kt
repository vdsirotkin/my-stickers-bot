package com.vdsirotkin.telegram.mystickersbot.admin

import com.github.fonimus.ssh.shell.SshShellHelper
import com.github.fonimus.ssh.shell.commands.SshShellComponent
import com.pengrad.telegrambot.request.GetChat
import com.pengrad.telegrambot.request.SendMessage
import com.vdsirotkin.telegram.mystickersbot.bot.MyStickersBot
import org.springframework.shell.standard.ShellCommandGroup
import org.springframework.shell.standard.ShellMethod

@SshShellComponent
@ShellCommandGroup("telegram-messaging")
class MessagingAdminService(
    private val bot: MyStickersBot,
    private val helper: SshShellHelper,
) {

    @ShellMethod
    fun sendMessage(chatId: Long) {
        val message = helper.read("Please input message:")

        helper.confirm("You are going to send message: '${message}'. Please confirm")

        val result = bot.execute(SendMessage(chatId, message))

        helper.print("Send result: $result")
    }

    @ShellMethod
    fun searchUser(chatId: Long) {
        val chat = bot.execute(GetChat(chatId))

        helper.print("""
            Username: ${chat?.chat()?.username()}
            First name: ${chat?.chat()?.firstName()}
            Last name: ${chat?.chat()?.lastName()}
        """.trimIndent())
    }
}
