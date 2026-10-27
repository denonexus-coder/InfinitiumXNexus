package com.azard_infinitium3d.server

import net.minecraft.server.MinecraftServer
import net.minecraft.world.scores.Scoreboard

/**
 * Ponte de leitura/escrita dos scores do datapack Infinitium.
 *
 * Estes sao OS objectives que o proprio datapack cria em setup/init.mcfunction
 * e consome em attr/spend, guide/tick, hud/toggle, etc. O mod apenas os le
 * (para mostrar) ou escreve um deles (inf.usep) seguindo o caminho do datapack.
 */
object ScoreIO {
	// --- FASE 1: atributos (setup/init.mcfunction) ---
	const val PTS = "inf.pts"
	const val STR = "inf.str"
	const val AGI = "inf.agi"
	const val VIT = "inf.vit"
	const val INT = "inf.int"
	const val MIN = "inf.min"
	const val LCK = "inf.lck"

	// --- Triggers documentados (o mod dispara exatamente estes) ---
	const val ATTR = "inf.attr" // /trigger inf.attr set 1-6
	const val HELP = "inf.help" // /trigger inf.help set 1-5

	// --- HUD ---
	const val MODE = "inf.mode" // 0=Status 1=Exploracao 2=Combate 3=RPG
	const val USEP = "inf.usep" // minecraft.used:carrot_on_a_stick (Painel)

	fun scoreboard(server: MinecraftServer): Scoreboard = server.scoreboard

	/** O datapack Infinitium esta ativo? (inf.pts so existe depois do setup) */
	fun hasPack(server: MinecraftServer): Boolean =
		scoreboard(server).getObjective(PTS) != null

	/** Le um score do jogador; 0 se o objective ou o score nao existirem. */
	fun read(board: Scoreboard, owner: String, objective: String): Int {
		val obj = board.getObjective(objective) ?: return 0
		if (!board.hasPlayerScore(owner, obj)) return 0
		return board.getOrCreatePlayerScore(owner, obj).score
	}

	/** Escreve um score do jogador (usado apenas para inf.usep). */
	fun write(board: Scoreboard, owner: String, objective: String, value: Int) {
		val obj = board.getObjective(objective) ?: return
		board.getOrCreatePlayerScore(owner, obj).score = value
	}
}
