package com.azard_infinitium3d.server

import com.azard_infinitium3d.net.GuidePacket
import com.azard_infinitium3d.net.HudCyclePacket
import com.azard_infinitium3d.net.SpendPacket
import com.azard_infinitium3d.net.StatePacket
import com.azard_infinitium3d.net.StateRequestPacket
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Nucleo servidor do InfinitiumCore.
 *
 * Contacto total com o datapack, sem nunca inventar regras:
 *  - spend  -> /trigger inf.attr set <1-6>  (o proprio attr/spend.mcfunction)
 *  - guide  -> /trigger inf.help set <1-5|99> (o proprio guide/menu.mcfunction)
 *  - hud    -> escreve inf.usep=1; o tick do datapack despacha hud/toggle,
 *              exatamente como o uso do Painel (vara-cenoura) faz.
 *
 * O estado devolvido ao cliente e lido diretamente dos scores `inf.*`.
 * Tudo corre na thread do servidor, com anti-spam e validacao de ranges.
 */
object ServerCore {
	private val LOGGER = LoggerFactory.getLogger("infinitiumcore")

	/** Intervalo minimo entre acoes do mesmo jogador (pacotes maliciosos/eco). */
	private const val MIN_INTERVAL_MS = 120L

	private val lastAction = HashMap<UUID, Long>()
	private val pendingSync = LinkedHashSet<UUID>()

	fun register() {
		ServerPlayNetworking.registerGlobalReceiver(StateRequestPacket.TYPE) { _, player, _ ->
			onServerThread(player) { sendState(player) }
		}

		ServerPlayNetworking.registerGlobalReceiver(SpendPacket.TYPE) { packet, player, _ ->
			onServerThread(player) { handleSpend(player, packet.attr) }
		}

		ServerPlayNetworking.registerGlobalReceiver(GuidePacket.TYPE) { packet, player, _ ->
			onServerThread(player) { handleGuide(player, packet.section) }
		}

		ServerPlayNetworking.registerGlobalReceiver(HudCyclePacket.TYPE) { _, player, _ ->
			onServerThread(player) { handleHudCycle(player) }
		}

		// Estado inicial a entrada (o cliente tambem pede ao abrir os ecraos).
		ServerPlayConnectionEvents.JOIN.register { handler, _, server ->
			server.execute { sendState(handler.player) }
		}

		// Depois do tick do mundo (onde o datapack despacha os triggers),
		// enviamos o estado ja atualizado para os jogadores afetados.
		ServerTickEvents.END_SERVER_TICK.register { server -> flushSync(server) }

		LOGGER.info("InfinitiumCore: protocolo registado (estado/gasto/guia/HUD).")
	}

	/** Garante que a acao corre na thread do servidor (packets chegam da netty). */
	private fun onServerThread(player: ServerPlayer, action: () -> Unit) {
		player.server.execute { action() }
	}

	private fun rateLimited(player: ServerPlayer): Boolean {
		val now = System.currentTimeMillis()
		val last = lastAction[player.uuid]
		if (last != null && now - last < MIN_INTERVAL_MS) return true
		lastAction[player.uuid] = now
		return false
	}

	private fun handleSpend(player: ServerPlayer, attr: Int) {
		if (attr !in 1..6) return
		val server = player.server
		if (!ScoreIO.hasPack(server)) {
			// Sem datapack: devolve o estado para o cliente mostrar o aviso.
			sendState(player)
			return
		}
		if (rateLimited(player)) return

		// O MESMO caminho que o jogador usaria a mao (compatibilidade total).
		server.commands.performPrefixedCommand(
			player.createCommandSourceStack(),
			"trigger inf.attr set $attr",
		)
		pendingSync += player.uuid
		LOGGER.debug("InfinitiumCore: {} gastou ponto no atributo {}", player.scoreboardName, attr)
	}

	private fun handleGuide(player: ServerPlayer, section: Int) {
		if (section !in 1..5 && section != GuidePacket.SECTION_MENU) return
		val server = player.server
		if (!ScoreIO.hasPack(server)) {
			sendState(player)
			return
		}
		if (rateLimited(player)) return

		server.commands.performPrefixedCommand(
			player.createCommandSourceStack(),
			"trigger inf.help set $section",
		)
	}

	private fun handleHudCycle(player: ServerPlayer) {
		val server = player.server
		if (!ScoreIO.hasPack(server)) {
			sendState(player)
			return
		}
		if (rateLimited(player)) return

		val board = ScoreIO.scoreboard(server)
		if (ScoreIO.objective(board, ScoreIO.USEP) == null) return

		// Simula 1 uso do Painel: o proprio tick do datapack despacha
		// `execute as @a[scores={inf.usep=1..}] run function infinitium:hud/toggle`.
		ScoreIO.write(board, player.scoreboardName, ScoreIO.USEP, 1)
		pendingSync += player.uuid
		LOGGER.debug("InfinitiumCore: {} pediu ciclo de HUD", player.scoreboardName)
	}

	/** Envia o estado lido dos scores do datapack para o jogador. */
	fun sendState(player: ServerPlayer) {
		if (!ServerPlayNetworking.canSend(player, StatePacket.TYPE)) return

		val server = player.server
		val board = ScoreIO.scoreboard(server)
		val owner = player.scoreboardName

		ServerPlayNetworking.send(
			player,
			StatePacket(
				ScoreIO.hasPack(server),
				ScoreIO.read(board, owner, ScoreIO.PTS),
				ScoreIO.read(board, owner, ScoreIO.STR),
				ScoreIO.read(board, owner, ScoreIO.AGI),
				ScoreIO.read(board, owner, ScoreIO.VIT),
				ScoreIO.read(board, owner, ScoreIO.INT),
				ScoreIO.read(board, owner, ScoreIO.MIN),
				ScoreIO.read(board, owner, ScoreIO.LCK),
				ScoreIO.read(board, owner, ScoreIO.MODE),
			),
		)
	}

	private fun flushSync(server: MinecraftServer) {
		if (pendingSync.isEmpty()) return
		val players = server.playerList.players
		for (uuid in pendingSync) {
			val player = players.firstOrNull { it.uuid == uuid } ?: continue
			sendState(player)
		}
		pendingSync.clear()
	}
}
