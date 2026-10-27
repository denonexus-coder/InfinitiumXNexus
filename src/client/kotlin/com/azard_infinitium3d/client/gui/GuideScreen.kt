package com.azard_infinitium3d.client.gui

import com.azard_infinitium3d.client.ClientState
import com.azard_infinitium3d.net.GuidePacket
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/**
 * Guia do Infinitium (tecla U).
 *
 * Layout (painel escuro com barra verde, grelha 2x3):
 *
 *   +--------------------------------------------------+
 *   |########### INFINITIUM - GUIA (dourado) ##########|
 *   |   Secoes escrevem no chat (texto do datapack)    |
 *   |  [1. Atributos  ]   [2. Classe & XP]             |
 *   |  [3. HUD        ]   [4. Mundo      ]             |
 *   |  [5. Comandos   ]   [Menu principal]             |
 *   |  [Atributos           ]  [Fechar]                |
 *   +--------------------------------------------------+
 *
 * Cada botao dispara o MESMO trigger do datapack:
 *   /trigger inf.help set <1-5>   (guide/menu.mcfunction)
 *   /trigger inf.help set 99      (so o cabecalho do menu - suportado pelo
 *                                  proprio datapack, que imprime o menu
 *                                  mesmo com numero invalido)
 * e abre o chat para leitura imediata da resposta do datapack.
 */
class GuideScreen : Screen(Component.literal("Infinitium - Guia")) {

	private val sections = arrayOf(
		1 to "1. Atributos",
		2 to "2. Classe & XP",
		3 to "3. HUD",
		4 to "4. Mundo",
		5 to "5. Comandos",
	)

	private var left = 0
	private var top = 0

	private val panelW = 250
	private val cellW = 120
	private val rowsTop get() = top + 40
	private val footerY get() = rowsTop + 3 * 24 + 12
	private val panelBottom get() = footerY + 26

	override fun init() {
		left = width / 2 - panelW / 2
		top = maxOf(6, (height - 174) / 2)

		// Grelha 2 colunas x 3 linhas (5 seccoes + menu principal).
		val items: List<Pair<Int, String>> = sections.toList() +
			(GuidePacket.SECTION_MENU to "Menu principal")

		items.forEachIndexed { i, (section, label) ->
			val col = i % 2
			val row = i / 2
			addRenderableWidget(
				Button.builder(Component.literal(label)) { _ -> open(section) }
					.bounds(left + col * (cellW + 10), rowsTop + row * 24, cellW, 20)
					.build(),
			)
		}

		addRenderableWidget(Button.builder(Component.literal("Atributos")) { _ ->
			minecraft?.setScreen(AttributeScreen())
		}.bounds(left, footerY, 120, 20).build())

		addRenderableWidget(Button.builder(Component.literal("Fechar")) { _ ->
			onClose()
		}.bounds(left + 130, footerY, 120, 20).build())
	}

	private fun open(section: Int) {
		if (ClientState.capable) {
			// Servidor com o mod: pacote validado -> /trigger inf.help set N.
			ClientPlayNetworking.send(GuidePacket(section))
		} else {
			// Servidor sem o mod: o cliente dispara o MESMO trigger vanilla.
			minecraft?.player?.connection?.sendCommand("trigger inf.help set $section")
		}
		// O datapack responde no chat: abre o chat para leitura imediata.
		minecraft?.setScreen(ChatScreen(""))
	}

	override fun render(g: GuiGraphics, mouseX: Int, mouseY: Int, tickDelta: Float) {
		renderBackground(g)

		// Painel + barra de cabecalho (.toInt() garante Int nos dois sentidos)
		g.fill(left - 12, top - 8, left + panelW + 12, panelBottom, 0xD0101018.toInt())
		g.fill(left - 12, top - 8, left + panelW + 12, top + 4, 0xFF1B5E20.toInt())

		super.render(g, mouseX, mouseY, tickDelta)

		drawCentered(g, "INFINITIUM - GUIA", top - 4, 0xFFFF55)

		if (ClientState.capable) {
			drawCentered(g, "Secoes escrevem no chat (texto do datapack)", top + 10, 0xAAAAAA)
		} else {
			drawCentered(g, "Servidor sem InfinitiumCore - /trigger vanilla", top + 10, 0xFF5555)
		}
	}

	private fun drawCentered(g: GuiGraphics, text: String, y: Int, color: Int) {
		g.drawString(font, text, width / 2 - font.width(text) / 2, y, color, true)
	}
}
