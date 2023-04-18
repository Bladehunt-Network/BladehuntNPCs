package net.bladehunt.npc

import net.bladehunt.npc.npc.PacketNPC
import net.minestom.server.instance.Instance

object NPCManager {
    private val npcs = hashSetOf<PacketNPC>()
    fun getNPCs(instance: Instance, chunkX: Int, chunkZ: Int): Collection<PacketNPC> {
        return npcs.filter { it.instance == instance && it.getPosition().chunkX() == chunkX  && it.getPosition().chunkZ() == chunkZ }
    }
    fun getNPCs(instance: Instance): Collection<PacketNPC> {
        return npcs.filter { it.instance == instance }
    }
    fun getNPC(id: Int): PacketNPC? {
        return npcs.firstOrNull { it.entityID == id }
    }
    fun getNPC(name: String): PacketNPC? {
        return npcs.firstOrNull { it.name == name }
    }
    fun getNPCs(): Collection<PacketNPC> {
        return npcs
    }
    fun addNPC(npc: PacketNPC) {
        npcs.add(npc)
        for (player in npc.instance.players) {
            npc.addViewer(player)
        }
    }
    fun removeNPC(npc: PacketNPC) {
        npcs.remove(npc)
    }
}