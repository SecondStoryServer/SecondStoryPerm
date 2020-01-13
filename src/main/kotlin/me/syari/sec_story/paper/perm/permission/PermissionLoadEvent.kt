package me.syari.sec_story.paper.perm.permission

import me.syari.sec_story.paper.library.event.CustomEvent
import org.bukkit.entity.Player

class PermissionLoadEvent(val player: Player): CustomEvent() {
    val permission get() = mutableList.toList()

    private val mutableList = mutableListOf<String>()

    fun addPermission(collection: Collection<String>) {
        mutableList.addAll(collection)
    }
}