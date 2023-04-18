package net.bladehunt.npc.action

import net.minestom.server.entity.Player

abstract class ClickAction(val clickType: ClickType) {
    enum class ClickType {
        LEFT,RIGHT,BOTH
    }
    abstract fun run(player: Player)
}