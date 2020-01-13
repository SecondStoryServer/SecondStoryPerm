package me.syari.sec_story.paper.perm.permission

import com.comphenix.protocol.utility.Util.getOnlinePlayers
import me.syari.sec_story.paper.library.command.CreateCommand.createCmd
import me.syari.sec_story.paper.library.command.CreateCommand.element
import me.syari.sec_story.paper.library.command.CreateCommand.offlinePlayers
import me.syari.sec_story.paper.library.command.CreateCommand.onlinePlayers
import me.syari.sec_story.paper.library.command.CreateCommand.tab
import me.syari.sec_story.paper.library.init.FunctionInit
import me.syari.sec_story.paper.library.message.SendMessage.send
import me.syari.sec_story.paper.library.player.UUIDPlayer
import me.syari.sec_story.paper.perm.Main.Companion.plugin
import me.syari.sec_story.paper.perm.sql.SQL.sql
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionAttachment

object Permission: FunctionInit {
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
                    s.appendln("&b[Perm] &fパーミッション一覧")
                    p.fromCache.forEach { s.appendln("&7- &a$it") }
                    sender.send(s)
                }
                "reload" -> {
                    val p = args.getPlayer(1, true)
                    if (p != null) {
                        p.loadPerm()
                        sender.send("&b[Perm] &a${p.name}&fのパーミッションをリロードしました")
                    } else {
                        getOnlinePlayers().forEach {
                            it.loadPerm()
                        }
                        sender.send("&b[Perm] &fオンラインプレイヤーのパーミッションをリロードしました")
                    }
                }
                else -> {
                    sender.send(
                        """
                        &b[Perm] &fコマンド一覧
                        &7- &a/perm add <Player> &7プレイヤーの権限を追加します
                        &7- &a/perm remove <Player> &7プレイヤーの権限を削除します
                        &7- &a/perm list <Player> &7プレイヤーの権限一覧を表示します
                        &7- &a/perm reload <Player> &7プレイヤーの権限をリロードします
                        &7- &a/perm reload &7全プレイヤーの権限をリロードします
                    """.trimIndent()
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
            return perm
        }

    private val cacheMap = mutableMapOf<UUIDPlayer, Set<String>>()

    private val OfflinePlayer.fromCache: Set<String>
        get() {
            return cacheMap.getOrPut(UUIDPlayer(this)) {
                fromSQL
            }
        }

    private fun Player.loadPerm() {
        val perm = addAttachment(plugin)
        perm.setPermission("*", false)
        val e = PermissionLoadEvent(this)
        e.callEvent()
        e.permission.forEach { p ->
            perm.setPermission(p, true)
        }
        fromSQL.forEach { p ->
            perm.setPermission(p, true)
        }
        val uuidPlayer = UUIDPlayer(this)
        if(perms.containsKey(uuidPlayer)) {
            unloadPerm()
        }
        perms[uuidPlayer] = perm
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
}