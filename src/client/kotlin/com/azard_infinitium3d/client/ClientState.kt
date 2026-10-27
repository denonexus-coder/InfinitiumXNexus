package com.azard_infinitium3d.client

import com.azard_infinitium3d.net.StatePacket
import com.azard_infinitium3d.net.StateRequestPacket
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking

/**
 * Estado do Infinitium visto pelo cliente.
 *
 * [state] vem sempre do SERVIDOR (scores do datapack) - o cliente nao calcula
 * nada. [capable] reflete se o servidor tambem tem o InfinitiumCore instalado;
 * sem ele, os ecraos caem para o caminho vanilla /trigger do proprio cliente.
 */
object ClientState {
	/** Ultimo estado recebido do servidor (null = ainda nao sincronizado). */
	var state: StatePacket? = null
		private set

	/** O servidor expoe o protocolo do InfinitiumCore? */
	val capable: Boolean
		get() = try {
			ClientPlayNetworking.canSend(StateRequestPacket.TYPE)
		} catch (_: Exception) {
			false
		}

	fun onState(packet: StatePacket) {
		state = packet
	}

	fun reset() {
		state = null
	}

	fun request() {
		if (capable) {
			ClientPlayNetworking.send(StateRequestPacket())
		}
	}
}
