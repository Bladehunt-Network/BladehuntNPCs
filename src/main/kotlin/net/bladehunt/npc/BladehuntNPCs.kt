package net.bladehunt.npc

import com.google.gson.GsonBuilder
import net.bladehunt.npc.command.NPCCommand
import net.bladehunt.npc.npc.NPCData
import net.bladehunt.npc.npc.NPCDataTypeAdapter
import net.bladehunt.npc.npc.PacketNPC
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.event.EventNode
import net.minestom.server.event.instance.AddEntityToInstanceEvent
import net.minestom.server.event.instance.InstanceChunkLoadEvent
import net.minestom.server.event.instance.InstanceChunkUnloadEvent
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent
import net.minestom.server.event.player.PlayerChunkLoadEvent
import net.minestom.server.event.player.PlayerChunkUnloadEvent
import net.minestom.server.event.player.PlayerPacketEvent
import net.minestom.server.extensions.Extension
import net.minestom.server.network.packet.client.play.ClientInteractEntityPacket
import net.minestom.server.network.packet.client.play.ClientInteractEntityPacket.Interact
import net.minestom.server.network.packet.client.play.ClientInteractEntityPacket.InteractAt
import net.minestom.server.timer.TaskSchedule
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import kotlin.io.path.absolutePathString

class BladehuntNPCs : Extension() {
    private val gson = GsonBuilder()
        .registerTypeAdapter(NPCData::class.java, NPCDataTypeAdapter())
        .setPrettyPrinting()
        .create()
    private val eventNode = EventNode.all("BladehuntNPCs")
    override fun initialize() {
        MinecraftServer.getCommandManager().register(NPCCommand())
        loadData().forEach { data ->
            val instance = MinecraftServer.getInstanceManager().getInstance(data.instanceUUID) ?: run {
                MinecraftServer.LOGGER.warn("Could not find instance with ID ${data.instanceUUID}")
                return@forEach
            }
            NPCManager.addNPC(PacketNPC(
                instance,
                data.position,
                data.id,
                data.lines.toMutableList(),
                data.type,
                data.skin,
                data.actions.toMutableList()
            ))
        }
        eventNode.addListener(InstanceChunkLoadEvent::class.java,this::loadChunk)
        eventNode.addListener(InstanceChunkUnloadEvent::class.java,this::unloadChunk)
        eventNode.addListener(RemoveEntityFromInstanceEvent::class.java,this::removeEntityFromInstance)
        eventNode.addListener(AddEntityToInstanceEvent::class.java,this::addEntityToInstance)
        eventNode.addListener(PlayerPacketEvent::class.java,this::playerPacketEvent)
        MinecraftServer.getGlobalEventHandler().addChild(eventNode)
    }

    override fun terminate() {
        saveData()
    }
    private fun removeEntityFromInstance(event: RemoveEntityFromInstanceEvent) {
        val player = event.entity as? Player ?: return
        NPCManager.getNPCs(event.instance).forEach { it.removeViewer(player) }
    }
    private fun addEntityToInstance(event: AddEntityToInstanceEvent) {
        val player = event.entity as? Player ?: return
        player.scheduler().scheduleTask({
            NPCManager.getNPCs(event.instance).forEach { it.addViewer(player) }
        }, TaskSchedule.seconds(1),TaskSchedule.stop())
    }
    private fun loadChunk(event: InstanceChunkLoadEvent) {
        NPCManager.getNPCs(event.instance,event.chunkX,event.chunkZ).forEach { npc ->
            event.instance.players.forEach { player ->
                npc.addViewer(player)
            }
        }
    }
    private fun unloadChunk(event: InstanceChunkUnloadEvent) {
        NPCManager.getNPCs(event.instance,event.chunkX,event.chunkZ).forEach { npc ->
            event.instance.players.forEach { player ->
                npc.removeViewer(player)
            }
        }
    }
    private fun playerPacketEvent(event: PlayerPacketEvent) {
        val packet = event.packet
        if (packet !is ClientInteractEntityPacket) return
        val npc = NPCManager.getNPC(packet.targetId)?:return
        val type = packet.type
        if ((type is Interact && type.hand == Player.Hand.OFF) || type is InteractAt) return
        if (type is Interact) npc.rightClick(event.player)
        else npc.leftClick(event.player)
    }

    fun loadData(): Collection<NPCData> {
        val file = File(dataDirectory.absolutePathString() + File.separator + "NPCs")
        val list = mutableListOf<NPCData>()
        if (!file.exists()) return list
        file.listFiles()?.forEach {
            if (it.isDirectory || it.name.contains(".DS_Store")) return@forEach
            list.add(gson.fromJson(FileReader(it),NPCData::class.java))
        }
        return list
    }
    fun saveData() {
        val file = File(dataDirectory.toFile().absolutePath+File.separator+"NPCs")
        if (!file.exists()) {
            file.mkdirs()
            file.mkdir()
        }
        file.listFiles()?.forEach {
            if (NPCManager.getNPC(it.nameWithoutExtension) == null) {
                it.delete()
            }
        }
        NPCManager.getNPCs().forEach {
            val f = File(file.absolutePath + File.separator + it.name + ".json")
            f.createNewFile()
            val writer = FileWriter(f)
            gson.toJson(it.toNPCData(), writer)
            writer.close()
        }
    }
}