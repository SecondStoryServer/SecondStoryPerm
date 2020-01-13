package me.syari.sec_story.paper.perm.sql

import me.syari.sec_story.paper.library.config.CreateConfig.config
import me.syari.sec_story.paper.library.message.SendMessage
import me.syari.sec_story.paper.library.sql.MySQL
import me.syari.sec_story.paper.perm.Main.Companion.plugin
import me.syari.sec_story.paper.perm.permission.Permission
import org.bukkit.command.CommandSender

object SQL {
    var sql: MySQL? = null

    fun loadConfig(output: CommandSender){
        config(plugin, output, "sql.yml", false){
            val host = getString("host")
            val port = getInt("port")
            val db = getString("database")
            val user = getString("user")
            val pass = getString("password")
            val mysql = MySQL.create(host, port, db, user, pass)
            if(mysql != null){
                val result = mysql.connectTest()
                if(result){
                    sql = mysql
                    SendMessage.sendConsole("&b[SQL] &fデータベースの接続に成功しました")
                    createSqlTable()
                } else {
                    SendMessage.sendConsole("&b[SQL] &cデータベースの接続に失敗しました")
                }
            } else {
                SendMessage.sendConsole("&b[SQL] &cデータベースの接続に必要な情報が足りませんでした")
            }
        }
    }

    fun createSqlTable(){
        Permission.createSqlTable()
    }
}