package me.syari.sec_story.paper.perm.permission

import com.comphenix.protocol.utility.Util.getOnlinePlayers
import me.syari.sec_story.paper.library.command.CreateCommand.createCmd
import me.syari.sec_story.paper.library.command.CreateCommand.element
import me.syari.sec_story.paper.library.command.CreateCommand.offlinePlayers
import me.syari.sec_story.paper.library.command.CreateCommand.onlinePlayers
import me.syari.sec_story.paper.library.command.CreateCommand.tab
import me.syari.sec_story.paper.library.init.EventInit
import me.syari.sec_story.paper.library.init.FunctionInit
import me.syari.sec_story.paper.library.message.SendMessage.send
import me.syari.sec_story.paper.library.player.UUIDPlayer
import me.syari.sec_story.paper.perm.Main.Companion.plugin
import me.syari.sec_story.paper.perm.sql.SQL.sql
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.permissions.PermissionAttachment

object Permission: FunctionInit, EventInit {
    override fun init() {
        createCmd(
            "perm", "Perm",
            tab { _, _ -> element("add", "remove", "list", "reload") },
            tab("add", "remove", "list") {  _, _ -> offlinePlayers },
            tab("reload") {  _, _ -> onlinePlayers }) { sender, args ->
            when (args.whenIndex(0)) {
                "add", "remove" -> {
                    val p = args.getOfflinePlayer(1, true) ?: return@createCmd
                    val perm = args.getOrNull(2) ?: return@createCmd sender.send("&b[Perm] &cパーミッションを入力してください")
                    when (args.whenIndex(0)) {
                        "add" -> {
                            if (p.containsSavePerm(perm)) return@createCmd sender.send("&b[Perm] &c既に権限が設定されています")
                            p.addSavePerm(perm)
                            sender.send("&b[Perm] &a${p.name}&fに&a${perm}&fを追加しました")
                        }
                        "remove" -> {
                            if (!p.containsSavePerm(perm)) return@createCmd sender.send("&b[Perm] &c権限が設定されていません")
                            p.removeSavePerm(perm)
                            sender.send("&b[Perm] &a${p.name}&fから&a${perm}&fを削除しました")
                        }
                    }
                }
                "list" -> {
                    val p = args.getOfflinePlayer(1, true) ?: return@createCmd
                    val s = StringBuilder()
                    sendList("パーミッション一覧", p.fromCache)
                }
                "reload" -> {
                    val p = args.getPlayer(1, true)
                    if (p != null) {
                        p.loadPerm()
                        sendWithPrefix("&a${p.name}&fのパーミッションをリロードしました")
                    } else {
                        getOnlinePlayers().forEach {
                            it.loadPerm()
                        }
                        sendWithPrefix("&fオンラインプレイヤーのパーミッションをリロードしました")
                    }
                }
                else -> {
                    sendHelp(
                        "perm add <Player>" to "プレイヤーの権限を追加します",
                        "perm remove <Player>" to "プレイヤーの権限を削除します",
                        "perm list <Player>" to "プレイヤーの権限一覧を表示します",
                        "perm reload <Player>" to "プレイヤーの権限をリロードします",
                        "perm reload" to "全プレイヤーの権限をリロードします"
                    )
                }
            }
        }
    }

    private val perms = mutableMapOf<UUIDPlayer, PermissionAttachment>()

    private fun OfflinePlayer.containsSavePerm(perm: String) = fromCache.contains(perm)

    private fun OfflinePlayer.addSavePerm(perm: String) {
        sql?.use {
            executeUpdate("INSERT INTO SS_Live_SS.Permission VALUE ('$uniqueId', '$perm')")
        }
        if(this is Player) loadPerm()
    }

    private fun OfflinePlayer.removeSavePerm(perm: String) {
        sql?.use {
            executeUpdate("DELETE FROM SS_Live_SS.Permission WHERE UUID = '$uniqueId' AND Perm = '$perm'")
        }
        if(this is Player) loadPerm()
    }

    private val OfflinePlayer.fromSQL: Set<String>
        get() {
            val perm = mutableSetOf<String>()
            sql?.use {
                val res = executeQuery("SELECT Perm FROM SS_Live_SS.Permission WHERE UUID = '$uniqueId'")
                while (res.next()) {
                    res.getString("Perm")?.let { perm.add(it) }
                }
            }
            cacheMap[UUIDPlayer(this)] = perm
            return perm
        }

    private val cacheMap = mutableMapOf<UUIDPlayer, Set<String>>()

    private val OfflinePlayer.fromCache: Set<String>
        get() {
            return cacheMap.getOrDefault(UUIDPlayer(this), fromSQL)
        }

    private fun Player.loadPerm() {
        val perm = addAttachment(plugin)
        perm.setPermission("*", false)
        val e = PermissionLoadEvent(this)
        e.callEvent()
        e.permission.forEach { p ->
            perm.setPermission(p, true)
        }
        val uuidPlayer = UUIDPlayer(this)
        if(perms.containsKey(uuidPlayer)) {
            unloadPerm()
        }
        perms[uuidPlayer] = perm
        PermissionLoadedEvent(this, perm.permissions.filter { it.value }.map { it.key }).callEvent()
    }

    private fun Player.unloadPerm() {
        perms.remove(UUIDPlayer(this))?.let {
            removeAttachment(it)
        }
    }

    fun createSqlTable(){
        sql?.use {
            executeUpdate(
                "CREATE TABLE IF NOT EXISTS Permission (UUID VARCHAR(36), Perm VARCHAR(255), PRIMARY KEY (UUID, Perm))"
            )
        }
    }

    @EventHandler
    fun on(e: PermissionLoadEvent){
        e.addPermission(e.player.fromSQL)
    }

    @EventHandler
    fun on(e: PlayerJoinEvent){
        e.player.loadPerm()
    }

    @EventHandler
    fun on(e: PlayerQuitEvent){
        e.player.unloadPerm()
    }

    fun onEnable(){
        plugin.server.onlinePlayers.forEach {
            it.loadPerm()
        }
    }
}