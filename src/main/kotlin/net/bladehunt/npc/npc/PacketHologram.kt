package net.bladehunt.npc.npc

import net.kyori.adventure.text.Component
import net.minestom.server.Viewable
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Metadata
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.network.packet.server.play.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class PacketHologram(var name: Component, var pos: Pos, val instance: Instance) : Viewable {
    val id = Entity.generateId()
    val uuid = UUID.randomUUID()
    private val viewers: MutableSet<Player> = ConcurrentHashMap.newKeySet()
    private val spawnEntityPacket get() = SpawnEntityPacket(
        id,
        uuid,
        EntityType.ARMOR_STAND.id(),
        pos,
        0f,
        0,
        0,
        0,
        0
    )
    private val entityMetaDataPacket get() = EntityMetaDataPacket(
        id,
        mapOf(
            0 to Metadata.Byte(0x20),
            2 to Metadata.OptChat(name),
            3 to Metadata.Boolean(true),
            5 to Metadata.Boolean(true),
            15 to Metadata.Byte(0x19)
        )
    )
    fun remove() {
        viewers.forEach {
            it.sendPacket(DestroyEntitiesPacket(id))
        }
        viewers.clear()
    }

    fun updateOldViewer(player: Player) {
        if (!viewers.contains(player)) return
        player.sendPacket(DestroyEntitiesPacket(id))
        player.sendPacket(spawnEntityPacket)
        player.sendPacket(entityMetaDataPacket)
    }

    override fun addViewer(player: Player): Boolean {
        if (viewers.contains(player) || player.instance != instance) return false
        viewers.add(player)
        player.sendPacket(spawnEntityPacket)
        player.sendPacket(entityMetaDataPacket)
        return true
    }

    override fun removeViewer(player: Player): Boolean {
        if (!viewers.contains(player)) return false
        viewers.remove(player)
        player.sendPacket(DestroyEntitiesPacket(id))
        return true
    }

    override fun getViewers(): MutableSet<Player> {
        return viewers
    }
}