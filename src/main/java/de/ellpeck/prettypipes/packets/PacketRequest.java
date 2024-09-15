package de.ellpeck.prettypipes.packets;

import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.terminal.ItemTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketRequest(BlockPos pos, ItemStack stack, int componentsHash, int amount) implements CustomPacketPayload {

    public static final Type<PacketRequest> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(PrettyPipes.ID, "request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketRequest> CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, PacketRequest::pos,
        ItemStack.STREAM_CODEC, PacketRequest::stack,
        ByteBufCodecs.INT, PacketRequest::componentsHash,
        ByteBufCodecs.INT, PacketRequest::amount,
        PacketRequest::new);

    public PacketRequest(BlockPos pos, ItemStack stack, int amount) {
        this(pos, stack, !stack.isComponentsPatchEmpty() ? stack.getComponents().hashCode() : 0, amount);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PacketRequest.TYPE;
    }

    public static void onMessage(PacketRequest message, IPayloadContext ctx) {
        var player = ctx.player();
        var tile = Utility.getBlockEntity(ItemTerminalBlockEntity.class, player.level(), message.pos);
        message.stack.setCount(message.amount);
        tile.requestItem(player, message.stack, message.componentsHash);

    }

}
