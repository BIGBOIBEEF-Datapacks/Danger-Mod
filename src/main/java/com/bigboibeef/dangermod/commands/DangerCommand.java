package com.bigboibeef.dangermod.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static com.bigboibeef.dangermod.DangerMod.*;

public class DangerCommand {
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("danger")
                    .then(ClientCommandManager.literal("add")
                            .then(ClientCommandManager.argument("name", StringArgumentType.string())
                                    .suggests((context, builder) -> {
                                        ClientPlayerEntity player = getPlayer();
                                        String typed = context.getInput().substring(context.getInput().lastIndexOf(' ') + 1).toLowerCase();
                                        for (PlayerEntity players : getClient().world.getPlayers()) {
                                            if (players.getName().getLiteralString().toLowerCase().startsWith(typed))
                                                builder.suggest(players.getName().getLiteralString());
                                        }
                                        return builder.buildFuture();
                                    })
                                    .executes(context -> {
                                        String name = StringArgumentType.getString(context, "name");

                                        if (name.equals(getPlayer().getName().getLiteralString())) {
                                            LOGGER.info("Failed to save " + name + " to list.(That's you!)");
                                            getClient().player.sendMessage(Text.literal("You cannot add yourself to your list.").styled(style -> style.withColor(Formatting.RED)));
                                            getPlayer().playSound(SoundEvents.ENTITY_VILLAGER_NO);
                                            return Command.SINGLE_SUCCESS;
                                        }

                                        addPlayer(name);
                                        return Command.SINGLE_SUCCESS;
                                    })
                            )
                    )
                    .then(ClientCommandManager.literal("list")
                            .executes(context -> {
                                listPlayers();
                                return Command.SINGLE_SUCCESS;
                            })
                    )
                    .then(ClientCommandManager.literal("remove")
                            .then(ClientCommandManager.argument("name", StringArgumentType.string())
                                    .suggests((context, builder) -> {
                                        ClientPlayerEntity player = getPlayer();
                                        String typed = context.getInput().substring(context.getInput().lastIndexOf(' ') + 1).toLowerCase();
                                        for (String players : getPlayers()) {
                                            if (players.toLowerCase().startsWith(typed))
                                                builder.suggest(players);
                                        }
                                        return builder.buildFuture();
                                    })
                                    .executes(context -> {
                                        String name = StringArgumentType.getString(context, "name");

                                        if (name.equals(getPlayer().getName().getLiteralString())) {
                                            LOGGER.info("Failed to save " + name + " to list.(That's you!)");
                                            getClient().player.sendMessage(Text.literal("You cannot add yourself to your list.").styled(style -> style.withColor(Formatting.RED)));
                                            getPlayer().playSound(SoundEvents.ENTITY_VILLAGER_NO);
                                            return Command.SINGLE_SUCCESS;
                                        }

                                        removePlayer(name);
                                        return Command.SINGLE_SUCCESS;
                                    })
                            )
                    )
            );
        });

    }
}