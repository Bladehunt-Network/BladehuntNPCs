package net.bladehunt.npc.action

import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player

class ClickCommand(clickType: ClickType, val command: String) : ClickAction(clickType) {
    override fun run(player: Player) {
        MinecraftServer.getCommandManager().execute(player,command)
    }

    override fun toString(): String {
        return "ClickCommand(command='$command', clickType=$clickType)"
    }
}