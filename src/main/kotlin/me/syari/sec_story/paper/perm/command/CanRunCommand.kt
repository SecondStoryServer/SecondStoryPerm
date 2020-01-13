package me.syari.sec_story.paper.perm.command

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary.getProtocolManager
import com.comphenix.protocol.events.ListenerPriority
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketContainer
import com.comphenix.protocol.events.PacketEvent
import com.comphenix.protocol.reflect.FieldAccessException
import me.syari.sec_story.paper.library.init.EventInit
import me.syari.sec_story.paper.library.init.FunctionInit
import me.syari.sec_story.paper.library.message.SendMessage.action
import me.syari.sec_story.paper.library.player.UUIDPlayer
import me.syari.sec_story.paper.perm.Main.Companion.plugin
import me.syari.sec_story.paper.perm.permission.PermissionLoadedEvent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import java.lang.reflect.InvocationTargetException

object CanRunCommand: FunctionInit, EventInit  {
    override fun init() {
        getProtocolManager().addPacketListener(object: PacketAdapter(
            plugin, ListenerPriority.NORMAL, PacketType.Play.Client.TAB_COMPLETE
        ) {
            @EventHandler
            override fun onPacketReceiving(e: PacketEvent) {
                if(e.packetType === PacketType.Play.Client.TAB_COMPLETE) {
                    try {
                        val p = e.player
                        if(p.isOp) return
                        val split = (e.packet.getSpecificModifier(String::class.java).read(0) as String).split("\\s+".toRegex(), 2)
                        val label = split[0].substring(1).toLowerCase()
                        val canList = p.canRunCommand
                        if(split.size == 1) {
                            val completions = PacketContainer(PacketType.Play.Server.TAB_COMPLETE)
                            completions.stringArrays.write(0, canList.filter { it.startsWith(label) }.map { "/$it" }.toTypedArray())
                            try {
                                getProtocolManager().sendServerPacket(p, completions)
                                e.isCancelled = true
                            } catch(e: InvocationTargetException) {
                                e.printStackTrace()
                            }
                            return
                        } else if(canList.contains(label)) {
                            return
                        }
                        e.isCancelled = true
                    } catch(e: FieldAccessException) {
                    }
                }
            }
        })
    }

    private val canRunCommandList = mutableMapOf<UUIDPlayer, List<String>>()

    private var Player.canRunCommand
        get() = canRunCommandList[UUIDPlayer(this)] ?: listOf()
        set(value) {
            val uuidPlayer = UUIDPlayer(this)
            canRunCommandList[uuidPlayer] = value
        }

    @EventHandler
    fun on(e: PermissionLoadedEvent){
        e.player.canRunCommand = e.permission.mapNotNull {
            if(it.matches("^command\\..+\$".toRegex())) it.substring(8).toLowerCase() else null
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun on(e: PlayerCommandPreprocessEvent){
        val p = e.player
        if(p.isOp) return
        val canList = p.canRunCommand
        val label = e.message.split(Regex("\\s+"), 2)[0].substring(1).toLowerCase()
        if(!canList.contains(label)){
            e.isCancelled = true
            p.action("&c&l&n実行できないコマンドです")
        }
    }
}