package com.bigboibeef.timetracker;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;

public class TimeTracker implements ClientModInitializer {
	public static final String MOD_ID = "time-tracker";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static KeyBinding TOGGLE_TIME;

	private static boolean showTime = false;

	private static long lastPressedTime = 0;
	private static final long COOLDOWN_TIME = 200;

	private static final Identifier CLOCK_TEXTURE = Identifier.of("time-tracker", "textures/gui/clock.png");

	private static int number = 1;

	@Override
	public void onInitializeClient() {
		HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
			if (!showTime) return;

			MinecraftClient client = MinecraftClient.getInstance();
			if (client.player == null || client.world == null) return;

			int screenWidth = drawContext.getScaledWindowWidth();
			int centerX    = screenWidth / 2;
			int size       = 32;
			int halfSize   = size / 2;

			drawContext.getMatrices().push();
			drawContext.getMatrices().translate(centerX, -halfSize, 0);
			drawContext.drawTexture(
					Identifier.of(MOD_ID, "textures/gui/clock_inner_" + number +".png"),
					-halfSize, 0,
					0, 0,
					size, size,
					size, size
			);
			drawContext.getMatrices().pop();

			drawContext.getMatrices().push();
			drawContext.getMatrices().translate(centerX, 0, 0);
			drawContext.drawTexture(
					Identifier.of(MOD_ID, "textures/gui/clock_outer.png"),
					-halfSize, 0,
					0, 0,
					size, size,
					size, size
			);
			drawContext.getMatrices().pop();
		});

		TOGGLE_TIME = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"Toggle Time",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_O,
				"Time Tracker"
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			long currentTime = System.currentTimeMillis();
			if (TOGGLE_TIME.isPressed() && currentTime - lastPressedTime > COOLDOWN_TIME) {
				if (client.player != null && client.world != null) {
					showTime = !showTime;
					lastPressedTime = currentTime;
					if (showTime) {
						client.player.sendMessage(Text.literal("[TIME] ")
								.setStyle(Style.EMPTY.withColor(0xEFB13C))
								.append(Text.literal("Clock On")
										.styled(s -> s.withColor(Formatting.GREEN))));
					} else {
						client.player.sendMessage(Text.literal("[TIME] ")
								.setStyle(Style.EMPTY.withColor(0xEFB13C))
								.append(Text.literal("Clock Off")
										.styled(s -> s.withColor(Formatting.GREEN))));
					}
					client.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP);
				}
			}
			if (client.world != null) {
				number = (((int) ((client.world.getTimeOfDay() + 1500) % 24000)) / 3000) + 1;
			}
		});
	}

	private static String formatTime(long time) {
		int hours = (int)((time / 1000 + 6) % 24);
		int minutes = (int)((time % 1000) * 60 / 1000);

		return String.format("Time: %02d:%02d", hours, minutes);
	}
}
