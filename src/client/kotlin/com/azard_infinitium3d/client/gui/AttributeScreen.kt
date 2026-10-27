package com.azard_infinitium3d.client.gui

import com.azard_infinitium3d.client.ClientState
import com.azard_infinitium3d.net.SpendPacket
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/**
 * Painel de Atributos (tecla O).
 *
 * Layout (painel escuro com barra verde, tipografia do datapack):
 *
 *   +--------------------------------------------------+
 *   |########## INFINITIUM - ATRIBUTOS (dourado) ######|
 *   |          Pontos de atributo: 3                   |
 *   |  FORCA        12                        [+1]    |
 *   |  AGILIDADE     5                        [+1]    |
 *   |  VITALIDADE    8                        [+1]    |
 *   |  INTELIGENCIA  3                        [+1]    |
 *   |  MINERACAO     7                        [+1]    |
 *   |  SORTE         0                        [+1]    |
 *   |  [Guia]  [Sincronizar]  [Fechar]                |
 *   |  Mesmo caminho: /trigger inf.attr set 1-6        |
 *   +--------------------------------------------------+
 *
 * Os valores vem do SERVIDOR (scores do datapack). O gasto usa o MESMO
 * trigger que o jogador escreveria: /trigger inf.attr set <1-6>.
 */
class AttributeScreen : Screen(Component.literal("Infinitium - Atributos")) {

	private val labels = arrayOf("FORCA", "AGILIDADE", "VITALIDADE", "INTELIGENCIA", "MINERACAO", "SORTE")
	private val colors = intArrayOf(0xFF5555, 0x55FF55, 0xFF55FF, 0x55FFFF, 0xFFFF55, 0x00AA00)

	private val spendButtons = arrayOfNulls<Button>(6)

	private var left = 0
	private var top = 0

	private val panelW = 250
	private val rowH = 20
	private val rowsTop get() = top + 40
	private val footerY get() = rowsTop + 6 * rowH + 6
	private val panelBottom get() = footerY + 34

	override fun init() {
		left = width / 2 - panelW / 2
		top = maxOf(6, (height - 208) / 2)

		for (i in 0..5) {
			val index = i
			val button = Button.builder(Component.literal("+1")) { _ ->
				spend(index + 1)
			}.bounds(left + panelW - 46, rowsTop + index * rowH, 42, 20).build()
			spendButtons[i] = button
			addRenderableWidget(button)
		}

		addRenderableWidget(Button.builder(Component.literal("Guia")) { _ ->
			minecraft?.setScreen(GuideScreen())
		}.bounds(left, footerY, 74, 20).build())

		addRenderableWidget(Button.builder(Component.literal("Sincronizar")) { _ ->
			ClientState.request()
		}.bounds(left + 80, footerY, 92, 20).build())

		addRenderableWidget(Button.builder(Component.literal("Fechar")) { _ ->
			onClose()
		}.bounds(left + 178, footerY, 72, 20).build())
	}

	private fun spend(attr: Int) {
		if (ClientState.capable) {
			// Servidor com o mod: pacote validado -> /trigger inf.attr set N.
			ClientPlayNetworking.send(SpendPacket(attr))
		} else {
			// Servidor sem o mod: o cliente dispara o MESMO trigger vanilla.
			minecraft?.player?.connection?.sendCommand("trigger inf.attr set $attr")
		}
	}

	override fun render(g: GuiGraphics, mouseX: Int, mouseY: Int, tickDelta: Float) {
		renderBackground(g)

		val state = ClientState.state
		val capable = ClientState.capable
		val hasPack = state?.hasPack == true
		val pts = state?.pts ?: 0

		// Botoes de gasto: ativos conforme o modo de operacao.
		val canSpend = when {
			!capable -> true          // modo compatibilidade: trigger vanilla do cliente
			state == null -> false    // ainda a sincronizar
			!hasPack -> false         // sem datapack nao ha pontos para gastar
			else -> pts > 0
		}
		for (button in spendButtons) button?.active = canSpend

		// Painel + barra de cabecalho (.toInt() garante Int nos dois sentidos)
		g.fill(left - 12, top - 8, left + panelW + 12, panelBottom, 0xD0101018.toInt())
		g.fill(left - 12, top - 8, left + panelW + 12, top + 4, 0xFF1B5E20.toInt())

		super.render(g, mouseX, mouseY, tickDelta)

		// Cabecalho
		drawCentered(g, "INFINITIUM - ATRIBUTOS", top - 4, 0xFFFF55)

		// Subtitulo / estado da ligacao
		when {
			!capable ->
				drawCentered(g, "Servidor sem InfinitiumCore - modo compatibilidade", top + 10, 0xFF5555)
			state == null ->
				drawCentered(g, "A sincronizar com o servidor...", top + 10, 0xFFFFAA)
			!hasPack ->
				drawCentered(g, "Datapack Infinitium nao detetado neste servidor", top + 10, 0xFF5555)
			else ->
				drawCentered(g, "Pontos de atributo: $pts", top + 10, 0xFFFF55)
		}

		// Linhas dos atributos (rotulo + valor alinhado a direita)
		for (i in 0..5) {
			val y = rowsTop + i * rowH + 6
			g.drawString(font, labels[i], left + 2, y, colors[i], true)

			val value = state?.let { attrValue(it, i) }?.toString() ?: "?"
			val valueX = left + panelW - 52 - font.width(value)
			g.drawString(font, value, valueX, y, 0xFFFFFF, true)
		}

		// Rodape: documenta o contacto servidor <-> datapack
		g.drawString(
			font,
			"Mesmo caminho: /trigger inf.attr set 1-6",
			left - 8,
			footerY + 24,
			0x808080,
			true,
		)
	}

	private fun attrValue(state: com.azard_infinitium3d.net.StatePacket, index: Int): Int =
		when (index) {
			0 -> state.str
			1 -> state.agi
			2 -> state.vit
			3 -> state.intel
			4 -> state.min
			else -> state.lck
		}

	private fun drawCentered(g: GuiGraphics, text: String, y: Int, color: Int) {
		g.drawString(font, text, width / 2 - font.width(text) / 2, y, color, true)
	}
}
