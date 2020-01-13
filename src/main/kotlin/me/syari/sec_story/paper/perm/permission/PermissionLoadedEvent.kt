package me.syari.sec_story.paper.perm.permission

import me.syari.sec_story.paper.library.event.CustomEvent
import org.bukkit.entity.Player

class PermissionLoadedEvent(val player: Player, val permission: List<String>): CustomEvent()