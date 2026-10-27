package com.azard_infinitium3d

import com.azard_infinitium3d.server.ServerCore
import net.fabricmc.api.ModInitializer
import net.minecraft.resources.ResourceLocation
import org.slf4j.LoggerFactory

/**
 * InfinitiumCore - companion do datapack Infinitium (Fabric 1.20.1).
 *
 * Regra de ouro: o datapack e o cerebro. Este mod NAO inventa regras:
 *  - le scores `inf.*` apenas para mostrar estado (leitura);
 *  - gasta pontos com o MESMO trigger documentado: /trigger inf.attr set <1-6>;
 *  - abre o guia com o MESMO trigger: /trigger inf.help set <1-5>;
 *  - alterna a HUD escrevendo inf.usep=1, exatamente como o uso do Painel
 *    (vara-cenoura) faz - o proprio tick do datapack despacha hud/toggle.
 *
 * Sem datapack, tudo continua a funcionar em modo compatibilidade vanilla.
 */
object Infinitiumcore : ModInitializer {
	const val MOD_ID: String = "infinitiumcore"

	private val LOGGER = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {
		ServerCore.register()
		LOGGER.info("InfinitiumCore carregado - triggers e scores do datapack Infinitium.")
	}

	fun id(path: String): ResourceLocation
		= ResourceLocation(MOD_ID, path)
}
