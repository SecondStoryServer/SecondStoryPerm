package me.syari.sec_story.paper.perm

import com.comphenix.protocol.ProtocolLibrary
import me.syari.sec_story.paper.library.init.EventInit
import me.syari.sec_story.paper.library.init.FunctionInit
import me.syari.sec_story.paper.perm.command.CanRunCommand
import me.syari.sec_story.paper.perm.config.Config
import me.syari.sec_story.paper.perm.permission.Permission
import org.bukkit.plugin.java.JavaPlugin

class Main: JavaPlugin() {
    companion object {
        lateinit var plugin: JavaPlugin
    }

    override fun onEnable() {
        plugin = this
        Config.load(server.consoleSender)
        ProtocolLibrary.getProtocolManager().addPacketListener(CanRunCommand)
        EventInit.register(this, CanRunCommand)
        FunctionInit.register(Permission)
    }
}