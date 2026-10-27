package com.azard_infinitium3d.client

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.KeyMapping
import org.lwjgl.glfw.GLFW

/**
 * Teclas do InfinitiumCore (todas rebindable em Controlos).
 *
 * O padrao do vanilla nao usa O/U/H, por isso nao ha conflito
 * (G, por exemplo, e "Trocar item da mao secundaria" por omissao).
 */
object ModKeys {
	private const val CATEGORY = "key.categories.infinitiumcore"

	val ATTRIBUTES: KeyMapping =
		KeyMapping("key.infinitiumcore.attributes", GLFW.GLFW_KEY_O, CATEGORY)

	val GUIDE: KeyMapping =
		KeyMapping("key.infinitiumcore.guide", GLFW.GLFW_KEY_U, CATEGORY)

	val HUD: KeyMapping =
		KeyMapping("key.infinitiumcore.hud", GLFW.GLFW_KEY_H, CATEGORY)

	fun register() {
		KeyBindingHelper.registerKeyBinding(ATTRIBUTES)
		KeyBindingHelper.registerKeyBinding(GUIDE)
		KeyBindingHelper.registerKeyBinding(HUD)
	}
}
