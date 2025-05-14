package com.bigboibeef.dangermod;

import com.bigboibeef.dangermod.commands.DangerCommand;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class DangerMod implements ClientModInitializer {
	public static final String MOD_ID = "danger-mod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static KeyBinding TOGGLE_CHECK;
	private static KeyBinding CHANGE_MODE;
	private static KeyBinding SHOW_BINDS;

	private static boolean showBinds;

	private static long lastPressedTime = 0;
	private static final long COOLDOWN_TIME = 200;

	private static int mode = 0;

	private static boolean checking;
	private static int count;

	private static Set<String> players = new HashSet<>();
	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.create();
	private static final Path SAVE_FILE = Paths.get("players.json");


	@Override
	public void onInitializeClient() {
		loadData();
		DangerCommand.register();
		count = 0;
		checking = false;
		showBinds = true;

		TOGGLE_CHECK = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"Toggle Check",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_I,
				"Danger Mod"
		));

		CHANGE_MODE = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"Change Mode",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_U,
				"Danger Mod"
		));

		SHOW_BINDS = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"Show Keybinds",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_MINUS,
				"Danger Mod"
		));


		HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
			int x = 10;
			int y = 10;
			int lineHeight = 10;

			if (showBinds) {
				drawContext.drawText(
						MinecraftClient.getInstance().textRenderer,
						"Toggle Check: " + DangerMod.TOGGLE_CHECK.getBoundKeyLocalizedText().getString(),
						x, y, 0xFFFFFF, true
				);

				drawContext.drawText(
						MinecraftClient.getInstance().textRenderer,
						"Change Mode: " + DangerMod.CHANGE_MODE.getBoundKeyLocalizedText().getString(),
						x, y + lineHeight, 0xFFFFFF, true
				);

				drawContext.drawText(
						MinecraftClient.getInstance().textRenderer,
						"Show Binds: " + DangerMod.SHOW_BINDS.getBoundKeyLocalizedText().getString(),
						x, y + 2 * lineHeight, 0xFFFFFF, true
				);

				int color = mode == 0 ? 0xff4c4c : (mode == 1 ? 0xffab17 : 0xffd817);
				String strMode = mode == 0 ? "Spam" : (mode == 1 ? "Single" : "IDK");//IF MORE MODES
				drawContext.drawText(
						MinecraftClient.getInstance().textRenderer,
						"Mode: " + strMode,
						x, y + 3 * lineHeight, color, true
				);

				color = checking ? 0x00FF00 : 0xFF5555;
				drawContext.drawText(
						MinecraftClient.getInstance().textRenderer,
						"Checking: " + checking,
						x, y + 4 * lineHeight, color, true
				);
			}
		});



		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			long currentTime = System.currentTimeMillis();

			if (currentTime - lastPressedTime > COOLDOWN_TIME) {
				if (SHOW_BINDS.isPressed()) {
					showBinds = !showBinds;
					lastPressedTime = currentTime;
					client.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP);
				}

				if (TOGGLE_CHECK.isPressed()) {
					checking = !checking;
					lastPressedTime = currentTime;
				}

				if (CHANGE_MODE.isPressed()) {
					mode++;
					if (mode == 2) {
						mode = 0;
					}
					lastPressedTime = currentTime;
				}//IF MORE MODES
			}

			if (checking) {
				if (mode == 0) {
					spam();
				} else if (mode == 1) {
					single();
				}//IF MORE MODES
			}

		});
	}

	public static void spam () {
		MinecraftClient client = getClient();

		count++;
		if (count % 20 == 0) {
			for (PlayerEntity player : client.world.getPlayers()) {
				if (player != client.player && !players.contains(player.getName().getLiteralString())) {
					client.player.sendMessage(Text.literal("⚠ Player nearby: " + player.getName().getLiteralString()).styled(style -> style.withColor(Formatting.RED)));
					client.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP);
				}
			}
		}
	}


	public static void single () {
		boolean toggle = false;
		MinecraftClient client = getClient();

		for (PlayerEntity player : client.world.getPlayers()) {
			if (player != client.player && !players.contains(player.getName().getLiteralString())) {
				client.player.sendMessage(Text.literal("⚠ Player nearby: " + player.getName().getLiteralString()).styled(style -> style.withColor(Formatting.RED)));
				client.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP);
				toggle = true;
			}
		}

		if (toggle) {
			checking = false;
		}
	}






	public static void addPlayer (String playerName) {
		loadData();
		if (players.contains(playerName)) {
			LOGGER.info("Failed to save " + playerName + " to list.(Already in list)");
			getClient().player.sendMessage(Text.literal("You already have " + playerName + " in your list.").styled(style -> style.withColor(Formatting.RED)));
			getPlayer().playSound(SoundEvents.ENTITY_VILLAGER_NO);
			return;
		}

		players.add(playerName);
		getClient().player.sendMessage(Text.literal("Successfully added " + playerName + " to your list.").styled(style -> style.withColor(Formatting.GREEN)));
		getPlayer().playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP);
		LOGGER.info("Saved " + playerName + " to list.");

		saveData();
	}

	public static void listPlayers () {
		if (players.isEmpty()) {
			LOGGER.info("Failed to list players. (No players in list)");
			getClient().player.sendMessage(Text.literal("No players in list. Add players using /danger add").styled(style -> style.withColor(Formatting.RED)));
			getPlayer().playSound(SoundEvents.ENTITY_VILLAGER_NO);
			return;
		}

		StringBuilder builder = new StringBuilder();

		for (String player : players) {
			builder.append(player).append(", ");
		}

		getClient().player.sendMessage(Text.literal(builder.substring(0, builder.length()-2)).styled(style -> style.withColor(Formatting.AQUA)));
		getPlayer().playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP);
		LOGGER.info("Successfully listed players.");
	}

	public static Set<String> getPlayers () {
		return players;
	}

	public static void removePlayer (String playerName) {
		loadData();
		if (!players.contains(playerName)) {
			LOGGER.info("Failed to remove " + playerName + " from list.(Already in list)");
			getClient().player.sendMessage(Text.literal("You do not have " + playerName + " in your list.").styled(style -> style.withColor(Formatting.RED)));
			getPlayer().playSound(SoundEvents.ENTITY_VILLAGER_NO);
			return;
		}

		players.remove(playerName);
		getClient().player.sendMessage(Text.literal("Successfully removed " + playerName + " from your list.").styled(style -> style.withColor(Formatting.GREEN)));
		getPlayer().playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP);
		LOGGER.info("Removed " + playerName + " from list.");

		saveData();
	}








	public static void loadData() {
		if (Files.exists(SAVE_FILE)) {
			try (Reader reader = Files.newBufferedReader(SAVE_FILE)) {
				Type type = new TypeToken<Set<String>>(){}.getType();
				Set<String> loaded = GSON.fromJson(reader, type);
				if (loaded != null) {
					players.clear();
					players.addAll(loaded);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void saveData() {
		try (Writer writer = Files.newBufferedWriter(SAVE_FILE)) {
			GSON.toJson(players, writer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}









	public static MinecraftClient getClient() {
		return MinecraftClient.getInstance();
	}

	public static ClientPlayerEntity getPlayer() {
		return getClient().player;
	}
}