package net.bladehunt.npc.npc

import net.bladehunt.npc.NPCManager
import net.bladehunt.npc.action.ClickAction
import net.bladehunt.npc.action.ClickCommand
import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.Viewable
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.*
import net.minestom.server.instance.Instance
import net.minestom.server.network.packet.server.play.*
import net.minestom.server.network.packet.server.play.PlayerInfoPacket.AddPlayer
import net.minestom.server.timer.ExecutionType
import net.minestom.server.timer.Task
import net.minestom.server.timer.TaskSchedule
import net.minestom.server.utils.PacketUtils
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class PacketNPC(
    val instance: Instance,
    private var position: Pos,
    val name: String,
    val lines: MutableList<Component>,
    val entityType: EntityType = EntityType.PLAYER,
    var playerSkin: PlayerSkin? = null,
    val actions: MutableList<ClickCommand> = arrayListOf()
) : Viewable {
    private val uuid = UUID.randomUUID()
    val entityID = Entity.generateId()

    fun getPosition(): Pos {
        return position
    }
    private val task: Task

    private val viewers: MutableSet<Player> = ConcurrentHashMap.newKeySet()

    companion object {
        val createTeamPacket = MinecraftServer.getTeamManager().createBuilder("bladehuntNPCs")
            .nameTagVisibility(TeamsPacket.NameTagVisibility.NEVER)
            .build()
            .createTeamsCreationPacket()
    }

    init {
        task = MinecraftServer.getSchedulerManager().buildTask {
            for (viewer in viewers) {
                val lookFromPos = position.add(0.0, entityType.height(), 0.0)
                val lookToPos = viewer.position.add(0.0, if (viewer.isSneaking) 1.5 else 1.8, 0.0)

                if (lookFromPos.distanceSquared(lookToPos) > 10*10) return@buildTask
                val pos = lookFromPos.withDirection(lookToPos.sub(lookFromPos))

                val lookPacket = EntityRotationPacket(entityID, pos.yaw, pos.pitch, true)
                val headLook = EntityHeadLookPacket(entityID, pos.yaw)
                viewer.sendPacket(lookPacket)
                viewer.sendPacket(headLook)
            }
        }.repeat(TaskSchedule.nextTick()).executionType(ExecutionType.ASYNC).schedule()
    }

    val hologram = PacketNPCHologram(this,lines)

    private val skin get() = if (playerSkin != null) listOf(
        AddPlayer.Property(
            "textures",
            playerSkin!!.textures(),
            playerSkin!!.signature()
        )
    ) else listOf()
    private val metaPacket = EntityMetaDataPacket(entityID, mapOf(17 to Metadata.Byte(0x7F)))
    private val teamPacket = TeamsPacket("bladehuntNPCs", TeamsPacket.AddEntitiesToTeamAction(listOf(name)))
    private val removeInfoPacket = PlayerInfoPacket(
        PlayerInfoPacket.Action.REMOVE_PLAYER,
        PlayerInfoPacket.RemovePlayer(uuid)
    )
    fun teleport(entity: Entity) {
        teleport(entity.position)
        for (viewer in entity.viewers) {
            addViewer(viewer)
        }
    }
    fun teleport(pos: Pos) {
        position = pos
        hologram.updatePosition()
        viewers.forEach {
            updateOldViewer(it)
            hologram.updateOldViewer(it)
        }
    }

    fun updateOldViewer(viewer: Player) {
        PacketUtils.sendGroupedPacket(viewers,DestroyEntitiesPacket(entityID))
        if (entityType == EntityType.PLAYER) {
            viewer.sendPacket(
                PlayerInfoPacket(
                    PlayerInfoPacket.Action.ADD_PLAYER,
                    listOf(
                        AddPlayer(
                            uuid,
                            name,
                            skin,
                            GameMode.CREATIVE,
                            0,
                            null,
                            null
                        ),
                    )
                )
            )
            viewer.sendPacket(SpawnPlayerPacket(entityID, uuid, position))
            viewer.sendPacket(metaPacket)
            viewer.sendPacket(createTeamPacket)
            viewer.sendPacket(teamPacket)
            viewer.scheduler().scheduleTask({viewer.sendPacket(removeInfoPacket)},
                TaskSchedule.tick(10),
                TaskSchedule.stop()
            )
        } else {
            viewer.sendPacket(SpawnEntityPacket(
                entityID,
                uuid,
                entityType.id(),
                position,
                0f,
                0,
                0,
                0,
                0)
            )
        }
        hologram.updateOldViewer(viewer)
    }

    override fun addViewer(viewer: Player): Boolean {
        if (viewers.contains(viewer) || viewer.instance != instance) return false
        viewers.add(viewer)
        updateOldViewer(viewer)
        hologram.addViewer(viewer)
        return true
    }

    override fun removeViewer(viewer: Player): Boolean {
        viewers.remove(viewer)

        viewer.sendPacket(DestroyEntitiesPacket(entityID))
        hologram.removeViewer(viewer)
        return true
    }

    fun remove() {
        PacketUtils.sendGroupedPacket(viewers, DestroyEntitiesPacket(entityID))
        viewers.clear()
        task.cancel()
        NPCManager.removeNPC(this)
    }

    fun leftClick(player: Player) {
        click(player)
        for (action in actions) {
            if (action.clickType != ClickAction.ClickType.LEFT) continue
            action.run(player)
        }
    }
    fun rightClick(player: Player) {
        click(player)
        for (action in actions) {
            if (action.clickType != ClickAction.ClickType.RIGHT) continue
            action.run(player)
        }
    }
    private fun click(player: Player) {
        for (action in actions) {
            if (action.clickType != ClickAction.ClickType.BOTH) continue
            action.run(player)
        }

    }


    override fun getViewers(): MutableSet<Player> {
        return viewers
    }

    fun toNPCData(): NPCData {
        return NPCData(
            name,
            entityType,
            instance.uniqueId,
            position,
            playerSkin,
            lines,
            actions
        )
    }
}