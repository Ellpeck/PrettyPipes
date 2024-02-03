package de.ellpeck.prettypipes.packets;

import de.ellpeck.prettypipes.PrettyPipes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.simple.SimpleChannel;
import net.neoforged.neoforge.network.NetworkRegistry;

public final class PacketHandler {

    private static final String VERSION = "1";
    private static SimpleChannel network;

    public static void setup() {
        PacketHandler.network = NetworkRegistry.newSimpleChannel(new ResourceLocation(PrettyPipes.ID, "network"), () -> PacketHandler.VERSION, PacketHandler.VERSION::equals, PacketHandler.VERSION::equals);
        PacketHandler.network.registerMessage(0, PacketItemEnterPipe.class, PacketItemEnterPipe::toBytes, PacketItemEnterPipe::fromBytes, PacketItemEnterPipe::onMessage);
        PacketHandler.network.registerMessage(1, PacketButton.class, PacketButton::toBytes, PacketButton::fromBytes, PacketButton::onMessage);
        PacketHandler.network.registerMessage(2, PacketNetworkItems.class, PacketNetworkItems::toBytes, PacketNetworkItems::fromBytes, PacketNetworkItems::onMessage);
        PacketHandler.network.registerMessage(3, PacketRequest.class, PacketRequest::toBytes, PacketRequest::fromBytes, PacketRequest::onMessage);
        PacketHandler.network.registerMessage(4, PacketGhostSlot.class, PacketGhostSlot::toBytes, PacketGhostSlot::fromBytes, PacketGhostSlot::onMessage);
        PacketHandler.network.registerMessage(5, PacketCraftingModuleTransfer.class, PacketCraftingModuleTransfer::toBytes, PacketCraftingModuleTransfer::fromBytes, PacketCraftingModuleTransfer::onMessage);
    }

    public static void sendToAllLoaded(Level world, BlockPos pos, Object message) {
        PacketHandler.network.send(PacketDistributor.TRACKING_CHUNK.with(() -> world.getChunkAt(pos)), message);
    }

    public static void sendTo(Player player, Object message) {
        PacketHandler.network.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), message);
    }

    public static void sendToServer(Object message) {
        PacketHandler.network.send(PacketDistributor.SERVER.noArg(), message);
    }
}
