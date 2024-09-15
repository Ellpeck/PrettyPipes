package de.ellpeck.prettypipes.packets;

import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.pipe.IPipeItem;
import de.ellpeck.prettypipes.pipe.PipeBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketItemEnterPipe(BlockPos tilePos, CompoundTag item) implements CustomPacketPayload {

    public static final Type<PacketItemEnterPipe> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(PrettyPipes.ID, "item_enter_pipe"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketItemEnterPipe> CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, PacketItemEnterPipe::tilePos,
        ByteBufCodecs.COMPOUND_TAG, PacketItemEnterPipe::item,
        PacketItemEnterPipe::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PacketItemEnterPipe.TYPE;
    }

    public static void onMessage(PacketItemEnterPipe message, IPayloadContext ctx) {
        var mc = Minecraft.getInstance();
        if (mc.level == null)
            return;
        var item = IPipeItem.load(message.item);
        var pipe = Utility.getBlockEntity(PipeBlockEntity.class, mc.level, message.tilePos);
        if (pipe != null)
            pipe.getItems().add(item);
    }

}
