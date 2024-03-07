package de.ellpeck.prettypipes.misc;

import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.network.PipeNetwork;
import de.ellpeck.prettypipes.packets.*;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

@Mod.EventBusSubscriber
public final class Events {

    @SubscribeEvent
    public static void onWorldCaps(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Registry.pipeConnectableCapability, Registry.pipeBlockEntity, (e, d) -> e);
        event.registerBlockEntity(Registry.pipeConnectableCapability, Registry.pressurizerBlockEntity, (e, d) -> e);
        event.registerBlockEntity(Registry.pipeConnectableCapability, Registry.itemTerminalBlockEntity, (e, d) -> e);
        event.registerBlockEntity(Registry.pipeConnectableCapability, Registry.craftingTerminalBlockEntity, (e, d) -> e);

        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, Registry.pressurizerBlockEntity, (e, d) -> e.storage);
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
                        source.sendSuccess(() -> Component.literal("Wrote network dump to file " + file.toAbsolutePath()), true);
                    } catch (IOException e) {
                        source.sendFailure(Component.literal("Failed to write network dump to file " + file.toAbsolutePath()));
                        return -1;
                    }
                    return 0;
                }))
                .then(Commands.literal("uncache").executes(c -> {
                    var source = c.getSource();
                    PipeNetwork.get(source.getLevel()).clearCaches();
                    source.sendSuccess(() -> Component.literal("Cleared all pipe caches in the world"), true);
                    return 0;
                }))
                .then(Commands.literal("unlock").executes(c -> {
                    var source = c.getSource();
                    PipeNetwork.get(source.getLevel()).unlock();
                    source.sendSuccess(() -> Component.literal("Resolved all network locks in the world"), true);
                    return 0;
                })));
    }

    @SubscribeEvent
    public static void onPayloadRegister(final RegisterPayloadHandlerEvent event) {
        var registrar = event.registrar(PrettyPipes.ID);
        registrar.play(PacketItemEnterPipe.ID, PacketItemEnterPipe::new, PacketItemEnterPipe::onMessage);
        registrar.play(PacketButton.ID, PacketButton::new, PacketButton::onMessage);
        registrar.play(PacketCraftingModuleTransfer.ID, PacketCraftingModuleTransfer::new, PacketCraftingModuleTransfer::onMessage);
        registrar.play(PacketGhostSlot.ID, PacketGhostSlot::new, PacketGhostSlot::onMessage);
        registrar.play(PacketNetworkItems.ID, PacketNetworkItems::new, PacketNetworkItems::onMessage);
        registrar.play(PacketRequest.ID, PacketRequest::new, PacketRequest::onMessage);
    }

}
