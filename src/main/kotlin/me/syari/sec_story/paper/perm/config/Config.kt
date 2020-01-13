package me.syari.sec_story.paper.perm.config

import me.syari.sec_story.paper.perm.sql.SQL
import org.bukkit.command.CommandSender

object Config {
    fun load(output: CommandSender){
        SQL.loadConfig(output)
    }
}