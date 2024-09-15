package de.ellpeck.prettypipes.packets;

import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.pipe.IPipeItem;
import de.ellpeck.prettypipes.pipe.PipeBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

public class PacketItemEnterPipe implements CustomPacketPayload {

    public static final ResourceLocation ID = new ResourceLocation(PrettyPipes.ID, "item_enter_pipe");

    private final BlockPos tilePos;
    private final CompoundTag item;

    public PacketItemEnterPipe(BlockPos tilePos, IPipeItem item) {
        this.tilePos = tilePos;
        this.item = item.serializeNBT();
    }

    public PacketItemEnterPipe(FriendlyByteBuf buf) {
        this.tilePos = buf.readBlockPos();
        this.item = buf.readNbt();
    }
    
    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.tilePos);
        buf.writeNbt(this.item);
    }

    @Override
    public ResourceLocation id() {
        return PacketItemEnterPipe.ID;
    }

    public static void onMessage(PacketItemEnterPipe message, PlayPayloadContext ctx) {
        ctx.workHandler().execute(() -> {
            var mc = Minecraft.getInstance();
            if (mc.level == null)
                return;
            var item = IPipeItem.load(message.item);
            var pipe = Utility.getBlockEntity(PipeBlockEntity.class, mc.level, message.tilePos);
            if (pipe != null)
                pipe.getItems().add(item);
        });
    }

}
