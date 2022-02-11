package de.ellpeck.prettypipes.misc;

import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.network.PipeNetwork;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

@Mod.EventBusSubscriber
public final class Events {

    @SubscribeEvent
    public static void onWorldCaps(AttachCapabilitiesEvent<Level> event) {
        event.addCapability(new ResourceLocation(PrettyPipes.ID, "network"), new PipeNetwork(event.getObject()));
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        event.getServer().getCommands().getDispatcher().register(Commands.literal(PrettyPipes.ID).requires(s -> s.hasPermission(2))
                .then(Commands.literal("dump").executes(c -> {
                    var source = c.getSource();
                    var file = Paths.get('_' + PrettyPipes.ID + "dump.txt");
                    var dump = PipeNetwork.get(source.getLevel()).toString();
                    try {
                        Files.writeString(file, dump, StandardCharsets.UTF_8);
                        source.sendSuccess(new TextComponent("Wrote network dump to file " + file.toAbsolutePath()), true);
                    } catch (IOException e) {
                        source.sendFailure(new TextComponent("Failed to write network dump to file " + file.toAbsolutePath()));
                        return -1;
                    }
                    return 0;
                }))
                .then(Commands.literal("uncache").executes(c -> {
                    var source = c.getSource();
                    PipeNetwork.get(source.getLevel()).clearCaches();
                    source.sendSuccess(new TextComponent("Cleared all pipe caches in the world"), true);
                    return 0;
                }))
                .then(Commands.literal("unlock").executes(c -> {
                    var source = c.getSource();
                    PipeNetwork.get(source.getLevel()).unlock();
                    source.sendSuccess(new TextComponent("Resolved all network locks in the world"), true);
                    return 0;
                })));
    }
}
