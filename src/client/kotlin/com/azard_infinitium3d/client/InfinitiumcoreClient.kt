package com.azard_infinitium3d.client

import com.azard_infinitium3d.client.gui.AttributeScreen
import com.azard_infinitium3d.client.gui.GuideScreen
import com.azard_infinitium3d.net.HudCyclePacket
import com.azard_infinitium3d.net.StatePacket
import com.azard_infinitium3d.net.StateRequestPacket
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.Minecraft
import org.slf4j.LoggerFactory

/**
 * Entrypoint de cliente do InfinitiumCore.
 *
 *  - Teclas: O = painel de atributos, U = guia, H = alternar modo do HUD.
 *  - Recebe o estado (scores do datapack) e mantem o ClientState atualizado.
 *  - Sem o mod no servidor, tudo cai para o caminho vanilla /trigger do cliente.
 */
object InfinitiumcoreClient : ClientModInitializer {
	private val LOGGER = LoggerFactory.getLogger("infinitiumcore")

	/** Re-sincronizacao automatica enquanto nao ha estado (ex.: logo apos entrar). */
	private const val AUTO_SYNC_MS = 1000L
	private var lastAutoRequest = 0L

	override fun onInitializeClient() {
		ModKeys.register()

		// Estado vindo do servidor (lido dos scores do datapack).
		ClientPlayNetworking.registerGlobalReceiver(StatePacket.TYPE) { packet, _, _ ->
			Minecraft.getInstance().execute { ClientState.onState(packet) }
		}

		ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
			ClientState.reset()
			ClientState.request()
			lastAutoRequest = System.currentTimeMillis()
		}

		ClientPlayConnectionEvents.DISCONNECT.register { _, _ -> ClientState.reset() }

		ClientTickEvents.END_CLIENT_TICK.register { client ->
			val player = client.player

			while (ModKeys.ATTRIBUTES.consumeClick()) {
				if (player != null) {
					ClientState.request()
					client.setScreen(AttributeScreen())
				}
			}

			while (ModKeys.GUIDE.consumeClick()) {
				if (player != null) {
					ClientState.request()
					client.setScreen(GuideScreen())
				}
			}

			while (ModKeys.HUD.consumeClick()) {
				// Sem o mod no servidor nao ha forma segura de alternar:
				// nesse caso usa-se o Painel (vara-cenoura) do datapack.
				if (player != null && ClientState.capable) {
					ClientPlayNetworking.send(HudCyclePacket())
				}
			}

			// Auto-sync: o handshake de canSend pode ficar pronto depois do JOIN.
			if (player != null && ClientState.state == null && ClientState.capable) {
				val now = System.currentTimeMillis()
				if (now - lastAutoRequest >= AUTO_SYNC_MS) {
					lastAutoRequest = now
					ClientState.request()
				}
			}
		}

		LOGGER.info("InfinitiumCore (cliente) pronto - teclas O/U/H registadas.")
	}
}
