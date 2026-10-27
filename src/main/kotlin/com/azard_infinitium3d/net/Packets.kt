package com.azard_infinitium3d.net

import com.azard_infinitium3d.Infinitiumcore
import net.fabricmc.fabric.api.networking.v1.FabricPacket
import net.fabricmc.fabric.api.networking.v1.PacketType
import net.minecraft.network.FriendlyByteBuf

/**
 * Protocolo InfinitiumCore (Fabric packets).
 *
 * O cliente nunca calcula nada: apenas pede estado e pede acoes que o SERVIDOR
 * traduz para os triggers/scores que o datapack Infinitium ja consome.
 * Todo o caminho e auditavel no ServerCore.
 */

/** C2S: cliente pede o estado atual (atributos, pontos, modo de HUD). */
class StateRequestPacket : FabricPacket {
	override fun write(buf: FriendlyByteBuf) {}
	override fun getType(): PacketType<*> = TYPE

	companion object {
		val TYPE: PacketType<StateRequestPacket> =
			PacketType.create(Infinitiumcore.id("state_request")) { _ -> StateRequestPacket() }
	}
}

/**
 * C2S: gastar 1 ponto no atributo [attr] (1=FOR 2=AGI 3=VIT 4=INT 5=MIN 6=LCK).
 * O servidor executa o MESMO `/trigger inf.attr set <N>` que o jogador escreveria.
 */
class SpendPacket(val attr: Int) : FabricPacket {
	override fun write(buf: FriendlyByteBuf) {
		buf.writeVarInt(attr)
	}

	override fun getType(): PacketType<*> = TYPE

	companion object {
		val TYPE: PacketType<SpendPacket> =
			PacketType.create(Infinitiumcore.id("spend")) { buf -> SpendPacket(buf.readVarInt()) }
	}
}

/**
 * C2S: abrir uma seccao do guia (1..5) ou o menu principal ([SECTION_MENU]).
 * O servidor executa o MESMO `/trigger inf.help set <N>` do datapack;
 * o valor invalido 99 e suportado pelo proprio datapack (o menu imprime o
 * cabecalho sempre que e despachado, conforme guide/menu.mcfunction).
 */
class GuidePacket(val section: Int) : FabricPacket {
	override fun write(buf: FriendlyByteBuf) {
		buf.writeVarInt(section)
	}

	override fun getType(): PacketType<*> = TYPE

	companion object {
		const val SECTION_MENU: Int = 99

		val TYPE: PacketType<GuidePacket> =
			PacketType.create(Infinitiumcore.id("guide")) { buf -> GuidePacket(buf.readVarInt()) }
	}
}

/**
 * C2S: alternar o modo do HUD. O servidor escreve `inf.usep = 1` (o mesmo que
 * o uso do Painel/vara-cenoura faz) e o tick do datapack despacha
 * `function infinitium:hud/toggle` - identico ao item, sem regras novas.
 */
class HudCyclePacket : FabricPacket {
	override fun write(buf: FriendlyByteBuf) {}
	override fun getType(): PacketType<*> = TYPE

	companion object {
		val TYPE: PacketType<HudCyclePacket> =
			PacketType.create(Infinitiumcore.id("hud_cycle")) { _ -> HudCyclePacket() }
	}
}

/**
 * S2C: estado lido DOS SCORES do datapack (somente leitura, zero calculo).
 * [hasPack] = datapack Infinitium detetado neste servidor.
 */
class StatePacket(
	val hasPack: Boolean,
	val pts: Int,
	val str: Int,
	val agi: Int,
	val vit: Int,
	val intel: Int,
	val min: Int,
	val lck: Int,
	val mode: Int,
) : FabricPacket {
	override fun write(buf: FriendlyByteBuf) {
		buf.writeVarInt(if (hasPack) 1 else 0)
		buf.writeVarInt(pts)
		buf.writeVarInt(str)
		buf.writeVarInt(agi)
		buf.writeVarInt(vit)
		buf.writeVarInt(intel)
		buf.writeVarInt(min)
		buf.writeVarInt(lck)
		buf.writeVarInt(mode)
	}

	override fun getType(): PacketType<*> = TYPE

	companion object {
		val TYPE: PacketType<StatePacket> =
			PacketType.create(Infinitiumcore.id("state")) { buf ->
				StatePacket(
					buf.readVarInt() != 0,
					buf.readVarInt(),
					buf.readVarInt(),
					buf.readVarInt(),
					buf.readVarInt(),
					buf.readVarInt(),
					buf.readVarInt(),
					buf.readVarInt(),
					buf.readVarInt(),
				)
			}
	}
}
