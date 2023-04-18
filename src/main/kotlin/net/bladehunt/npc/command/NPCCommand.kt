package net.bladehunt.npc.command

import net.bladehunt.npc.NPCManager
import net.bladehunt.npc.action.ClickAction
import net.bladehunt.npc.action.ClickCommand
import net.bladehunt.npc.npc.PacketNPC
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.CommandContext
import net.minestom.server.command.builder.arguments.ArgumentCommand
import net.minestom.server.command.builder.arguments.ArgumentEnum
import net.minestom.server.command.builder.arguments.ArgumentGroup
import net.minestom.server.command.builder.arguments.ArgumentLiteral
import net.minestom.server.command.builder.arguments.ArgumentStringArray
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerSkin

class NPCCommand : Command("npc","npcs","bladehuntnpcs") {
    private val selectedNPCs = hashMapOf<Player,PacketNPC>()
    private val nameArg = ArgumentType.String("name")
    private val indexArg = ArgumentType.Integer("index").between(0,5000)
    private val componentArg = ArgumentType.Component("component")

    private val clickTypeArg = ArgumentEnum("click-type",ClickAction.ClickType::class.java)
    private val commandArg = ArgumentStringArray("command")
    init {
        setCondition { sender, _ -> sender is Player }
        setDefaultExecutor(this::defaultExecutor)
        addSyntax(this::info)
        addSyntax(this::list,ArgumentLiteral("list"))
        addSyntax(this::info,ArgumentLiteral("info"))
        addSyntax(this::createNPC,ArgumentLiteral("create"), nameArg)
        addSyntax(this::selectNPC,ArgumentLiteral("select"))
        addSyntax(this::removeNPC,ArgumentLiteral("remove"))
        addSyntax(this::tpHere,ArgumentLiteral("tphere"))
        addSyntax(this::setSkin,ArgumentLiteral("skin"), nameArg)

        val textArg = ArgumentLiteral("text")
        addSyntax(this::setLine,textArg,ArgumentLiteral("set"),indexArg,componentArg)
        addSyntax(this::addLine,textArg,ArgumentLiteral("add"),componentArg)
        addSyntax(this::removeLine,textArg,ArgumentLiteral("remove"),indexArg)

        val commandArg = ArgumentLiteral("cmd")
        addSyntax(this::addAction,commandArg,ArgumentLiteral("add"),clickTypeArg,this.commandArg)
        addSyntax(this::removeAction,commandArg,ArgumentLiteral("remove"), indexArg)
        addSyntax(this::listActions,commandArg,ArgumentLiteral("list"))
    }
    private fun defaultExecutor(sender: CommandSender, context: CommandContext) {
        if (sender !is Player) return
        sender.sendMessage(Component.text(
            "Incorrect Usage!",
            NamedTextColor.RED
        ))
    }
    private fun list(sender: CommandSender, context: CommandContext) {
        var component = Component.text("NPCs:").appendNewline()
        for (npc in NPCManager.getNPCs()) {
            component = component.append(Component.text(npc.name)).appendNewline()
        }
        component = component.append(Component.text("Select an NPC and run /npc for more info."))
        sender.sendMessage(component)
    }
    private fun info(sender: CommandSender, context: CommandContext) {
        if (sender !is Player) return
        val npc = getSelectedNPC(sender) ?: return
        sender.sendMessage(Component.text("Info for ${npc.name}")
            .appendNewline()
            .append(Component.text("Lines:\n${npc.hologram.name.map { it.examinableName() + "\n" }}"))
            .appendNewline()
            .append(Component.text("Actions:\n${npc.actions.map { it.command + " - " + it.clickType.name + "\n" }}"))
            .appendNewline()
            .append(Component.text("Has skin: ${npc.playerSkin != null}"))
            .color(NamedTextColor.AQUA)
        )
    }
    private fun selectNPC(sender: CommandSender, context: CommandContext) {
        if (sender !is Player) return
        var closestNPC: PacketNPC? = null
        var closestDistance: Double = Double.MAX_VALUE
        for (npc in NPCManager.getNPCs(sender.instance!!)) {
            val distSq = npc.getPosition().distanceSquared(sender.position)
            if (distSq >= closestDistance) continue
            closestNPC = npc
            closestDistance = distSq
        }
        if (closestNPC == null) {
            sender.sendMessage(Component.text("Could not find an NPC to select.", NamedTextColor.RED))
            return
        }
        sender.sendMessage(Component.text("You selected ${closestNPC.name}",NamedTextColor.RED))
        selectedNPCs[sender] = closestNPC
    }
    private fun removeNPC(sender: CommandSender, context: CommandContext) {
        if (sender !is Player) return
        val selNPC = getSelectedNPC(sender)?:return
        selNPC.remove()
        selectedNPCs.remove(sender)
        sender.sendMessage(Component.text(
            "You removed ${selNPC.name}",
            NamedTextColor.RED
        ))
    }
    private fun createNPC(sender: CommandSender, context: CommandContext) {
        if (sender !is Player) return
        val name = context.get(nameArg)
        if (NPCManager.getNPC(name) != null) {
            sender.sendMessage(Component.text("An NPC named $name already exists!",NamedTextColor.RED))
            return
        }
        val npc = PacketNPC(
            sender.instance!!,
            sender.position,
            name,
            arrayListOf(),
            playerSkin = PlayerSkin.fromUuid("c887ffbd-bae3-4794-af3e-a20a63840e35")
        )
        sender.sendMessage(Component.text("Created NPC $name",NamedTextColor.RED))
        NPCManager.addNPC(npc)
        selectedNPCs[sender] = npc
    }
    private fun tpHere(sender: CommandSender, context: CommandContext) {
        if (sender !is Player) return
        val selNPC = getSelectedNPC(sender)?:return
        if (sender.instance != selNPC.instance) {
            sender.sendMessage(Component.text(
                "Your selected NPC is not in your instance!",
                NamedTextColor.RED
            ))
            return
        }
        selNPC.teleport(sender)
        sender.sendMessage(Component.text(
            "Teleported ${selNPC.name} to you.",
            NamedTextColor.GREEN
        ))
    }
    private fun setSkin(sender: CommandSender, context: CommandContext) {
        if (sender !is Player) return
        val selNPC = getSelectedNPC(sender)?:return
        val name = context.get(nameArg)
        val skin = PlayerSkin.fromUsername(name) ?: run {
            sender.sendMessage(Component.text("Could not find a skin for that player!",NamedTextColor.RED))
            return
        }
        selNPC.playerSkin = skin
        selNPC.viewers.forEach { selNPC.updateOldViewer(it) }
        sender.sendMessage(Component.text("Set ${selNPC.name}'s skin to $name", NamedTextColor.GREEN))
    }

    private fun setLine(sender: CommandSender, context: CommandContext) {
        if (sender !is Player) return
        val selNPC = getSelectedNPC(sender)?:return
        val index = context.get(indexArg)
        val component = context.get(componentArg)
        selNPC.hologram.setLine(index,component)
        sender.sendMessage(Component.text("Set line $index to ").append(component))
    }
    private fun addLine(sender: CommandSender, context: CommandContext) {
        if (sender !is Player) return
        val selNPC = getSelectedNPC(sender)?:return
        val component = context.get(componentArg)
        selNPC.hologram.addLine(component)
        sender.sendMessage(Component.text("Added line ").append(component))
    }
    private fun removeLine(sender: CommandSender, context: CommandContext) {
        if (sender !is Player) return
        val selNPC = getSelectedNPC(sender)?:return
        val index = context.get(indexArg)
        if (selNPC.hologram.name.size <= index) {
            sender.sendMessage(Component.text("There was no line at $index",NamedTextColor.RED))
            return
        }
        selNPC.hologram.removeLine(index)
        sender.sendMessage(Component.text("Removed line at $index", NamedTextColor.GREEN))
    }

    private fun addAction(sender: CommandSender, context: CommandContext) {
        if (sender !is Player) return
        val selNPC = getSelectedNPC(sender)?:return

        val input = context.get(commandArg).joinToString(" ")
        selNPC.actions.add(ClickCommand(context.get(clickTypeArg),input))
        sender.sendMessage(Component.text("Added command $input to ${selNPC.name}",NamedTextColor.GREEN))
    }
    private fun removeAction(sender: CommandSender, context: CommandContext) {
        if (sender !is Player) return
        val selNPC = getSelectedNPC(sender)?:return
        val index = context.get(indexArg)
        if (selNPC.actions.size <= index) {
            sender.sendMessage(Component.text("There was no action at $index",NamedTextColor.RED))
        }
        selNPC.actions.removeAt(index)
        sender.sendMessage(Component.text("Removed action at $index", NamedTextColor.GREEN))
    }
    private fun listActions(sender: CommandSender, context: CommandContext) {
        if (sender !is Player) return
        val selNPC = getSelectedNPC(sender)?:return
        val actions = selNPC.actions
        var component = Component.text("Actions:", NamedTextColor.AQUA)
        for (i in 0 until actions.size) {
            val action = actions[i]
            component = component.append(Component.text("$i - ${action.clickType.name} - ${action.command}"))
        }
        sender.sendMessage(component)
    }

    private fun getSelectedNPC(player: Player): PacketNPC? {
        return selectedNPCs[player] ?: run {
            player.sendMessage(Component.text(
                "You don't have an NPC selected!",
                NamedTextColor.RED
            ))
            null
        }
    }
}