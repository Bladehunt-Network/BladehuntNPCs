package net.bladehunt.npc.npc

import net.kyori.adventure.text.Component
import net.minestom.server.Viewable
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.entity.hologram.Hologram
import net.minestom.server.network.packet.server.play.*
import net.minestom.server.utils.PacketUtils
import java.util.concurrent.ConcurrentHashMap

class PacketNPCHologram(val npc: PacketNPC, val name: MutableList<Component>) : Viewable {
    constructor(npc: PacketNPC, vararg name: Component) : this(npc,name.toMutableList())
    private var index = 0
    private var holograms = name.map {
        index++
        PacketHologram(it,npc.getPosition().add(0.0,1.5+index*0.3,0.0),npc.instance)
    }.toMutableList()
    private val viewers: MutableSet<Player> = ConcurrentHashMap.newKeySet()

    fun updateOldViewer(player: Player) {
        for (hologram in holograms) {
            hologram.updateOldViewer(player)
        }
    }

    fun addLine(component: Component) {
        name.add(component)
        holograms.add(
            PacketHologram(component,npc.getPosition().add(0.0,1.8+holograms.size*0.3,0.0),npc.instance)
                .also { holo -> viewers.forEach { holo.addViewer(it) } }
        )
        updatePosition()
    }
    fun setLine(index: Int, component: Component) {
        holograms[index].name = component
        viewers.forEach { updateOldViewer(it) }
    }
    fun removeLine(index: Int) {
        viewers.forEach { holograms[index].removeViewer(it) }
        holograms.removeAt(index)
        name.removeAt(index)
        updatePosition()
        viewers.forEach { updateOldViewer(it) }
    }

    fun updatePosition() {
        index = 0
        holograms.forEach {
            index++
            it.pos = npc.getPosition().add(0.0,1.5+index*0.3,0.0)
        }
    }

    override fun addViewer(player: Player): Boolean {
        if (viewers.contains(player)) return false
        viewers.add(player)
        for (hologram in holograms) {
            hologram.addViewer(player)
        }
        return true
    }

    override fun removeViewer(player: Player): Boolean {
        if (!viewers.contains(player)) return false
        for (hologram in holograms) {
            hologram.removeViewer(player)
        }
        return true
    }

    override fun getViewers(): MutableSet<Player> {
        return viewers
    }
}