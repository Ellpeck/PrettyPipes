package de.ellpeck.prettypipes.packets;

import de.ellpeck.prettypipes.PrettyPipes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public final class PacketHandler {

    private static final String VERSION = "1";
    private static SimpleChannel network;

    public static void setup() {
        network = NetworkRegistry.newSimpleChannel(new ResourceLocation(PrettyPipes.ID, "network"), () -> VERSION, VERSION::equals, VERSION::equals);
        network.registerMessage(0, PacketItemEnterPipe.class, PacketItemEnterPipe::toBytes, PacketItemEnterPipe::fromBytes, PacketItemEnterPipe::onMessage);
        network.registerMessage(1, PacketButton.class, PacketButton::toBytes, PacketButton::fromBytes, PacketButton::onMessage);
        network.registerMessage(2, PacketNetworkItems.class, PacketNetworkItems::toBytes, PacketNetworkItems::fromBytes, PacketNetworkItems::onMessage);
        network.registerMessage(3, PacketRequest.class, PacketRequest::toBytes, PacketRequest::fromBytes, PacketRequest::onMessage);
        network.registerMessage(4, PacketGhostSlot.class, PacketGhostSlot::toBytes, PacketGhostSlot::fromBytes, PacketGhostSlot::onMessage);
        network.registerMessage(5, PacketCraftingModuleTransfer.class, PacketCraftingModuleTransfer::toBytes, PacketCraftingModuleTransfer::fromBytes, PacketCraftingModuleTransfer::onMessage);
    }

    public static void sendToAllLoaded(World world, BlockPos pos, Object message) {
        network.send(PacketDistributor.TRACKING_CHUNK.with(() -> world.getChunkAt(pos)), message);
    }

    public static void sendTo(PlayerEntity player, Object message) {
        network.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player), message);
    }

    public static void sendToServer(Object message) {
        network.send(PacketDistributor.SERVER.noArg(), message);
    }
}
