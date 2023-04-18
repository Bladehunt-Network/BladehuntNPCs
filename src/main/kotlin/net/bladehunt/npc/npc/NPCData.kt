package net.bladehunt.npc.npc

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import net.bladehunt.npc.action.ClickCommand
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.PlayerSkin
import java.util.UUID

data class NPCData(
    val id: String,
    val type: EntityType,
    val instanceUUID: UUID,
    val position: Pos,
    val skin: PlayerSkin?,
    val lines: List<Component>,
    val actions: List<ClickCommand>
) {
    override fun toString(): String {
        return "NPCData(id='$id', type=$type, instanceUUID=$instanceUUID, position=$position, skin=$skin, lines=$lines, actions=$actions)"
    }
}

class NPCDataTypeAdapter : TypeAdapter<NPCData>() {
    companion object {
        private val gson = GsonBuilder()
            .registerTypeAdapter(Pos::class.java,PosTypeAdapter())
            .registerTypeAdapter(PlayerSkin::class.java,SkinTypeAdapter())
            .create()
    }
    override fun write(writer: JsonWriter, value: NPCData) {
        writer.beginObject()
        writer.name("id")
        writer.value(value.id)
        writer.name("type")
        writer.value(value.type.id())
        writer.name("pos")
        gson.toJson(value.position,Pos::class.java,writer)
        writer.name("instance")
        writer.value(value.instanceUUID.toString())
        writer.name("skin")
        gson.toJson(value.skin,PlayerSkin::class.java,writer)
        writer.name("lines")
        writer.beginArray()
        for (line in value.lines) {
            GsonComponentSerializer
                .gson()
                .serializer()
                .toJson(line,TextComponent::class.java,writer)
        }
        writer.endArray()
        writer.name("actions")
        writer.beginArray()
        for (action in value.actions) {
            GsonComponentSerializer
                .gson()
                .serializer()
                .toJson(action,ClickCommand::class.java,writer)
        }
        writer.endArray()
        writer.endObject()
    }

    override fun read(reader: JsonReader): NPCData? {
        reader.beginObject()
        var id = ""
        var type: EntityType = EntityType.PLAYER
        var pos = Pos(0.0,0.0,0.0)
        var instance = UUID.randomUUID()
        var skin: PlayerSkin? = null
        val lines = arrayListOf<Component>()
        val actions = arrayListOf<ClickCommand>()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "id" -> id = reader.nextString()
                "type" -> type = EntityType.fromId(reader.nextInt())!!
                "pos" -> pos = gson.fromJson(reader,Pos::class.java)
                "skin" -> skin = gson.fromJson(reader,PlayerSkin::class.java)
                "instance" -> instance = UUID.fromString(reader.nextString())
                "lines" -> {
                    reader.beginArray()
                    while (reader.hasNext()) {
                        lines.add(GsonComponentSerializer
                            .gson()
                            .serializer()
                            .fromJson(reader,TextComponent::class.java)
                        )
                    }
                    reader.endArray()
                }
                "actions" -> {
                    reader.beginArray()
                    while (reader.hasNext()) {
                        actions.add(Gson().fromJson(reader,ClickCommand::class.java))
                    }
                    reader.endArray()
                }
            }
        }
        reader.endObject()
        return NPCData(
            id,
            type,
            instance,
            pos,
            skin,
            lines,
            actions
        )
    }
}
class PosTypeAdapter : TypeAdapter<Pos>() {
    override fun write(writer: JsonWriter, value: Pos) {
        writer.beginObject()

        writer.name("x")
        writer.value(value.x)
        writer.name("y")
        writer.value(value.y)
        writer.name("z")
        writer.value(value.z)

        writer.name("yaw")
        writer.value(value.yaw)
        writer.name("pitch")
        writer.value(value.pitch)

        writer.endObject()
    }

    override fun read(reader: JsonReader): Pos {
        reader.beginObject()
        var x = 0.0
        var y = 0.0
        var z = 0.0

        var yaw = 0f
        var pitch = 0f

        while (reader.hasNext()) {
            when (reader.nextName()) {
                "x" -> x = reader.nextDouble()
                "y" -> y = reader.nextDouble()
                "z" -> z = reader.nextDouble()

                "yaw" -> yaw = reader.nextDouble().toFloat()
                "pitch" -> pitch = reader.nextDouble().toFloat()
            }
        }
        reader.endObject()
        return Pos(x,y,z,yaw,pitch)
    }
}
class SkinTypeAdapter : TypeAdapter<PlayerSkin>() {
    override fun write(writer: JsonWriter, value: PlayerSkin) {
        writer.beginObject()
        writer.name("textures")
        writer.value(value.textures())
        writer.name("signature")
        writer.value(value.signature())
        writer.endObject()
    }

    override fun read(reader: JsonReader): PlayerSkin {
        reader.beginObject()
        var textures = ""
        var signature = ""
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "textures" -> textures = reader.nextString()
                "signature" -> signature = reader.nextString()
            }
        }
        reader.endObject()
        return PlayerSkin(textures,signature)
    }

}