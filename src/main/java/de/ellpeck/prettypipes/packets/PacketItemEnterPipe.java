package de.ellpeck.prettypipes.packets;

import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.pipe.IPipeItem;
import de.ellpeck.prettypipes.pipe.PipeBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketItemEnterPipe {

    private BlockPos tilePos;
    private CompoundTag item;

    public PacketItemEnterPipe(BlockPos tilePos, IPipeItem item) {
        this.tilePos = tilePos;
        this.item = item.serializeNBT();
    }

    private PacketItemEnterPipe() {

    }

    public static PacketItemEnterPipe fromBytes(FriendlyByteBuf buf) {
        var client = new PacketItemEnterPipe();
        client.tilePos = buf.readBlockPos();
        client.item = buf.readNbt();
        return client;
    }

    public static void toBytes(PacketItemEnterPipe packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.tilePos);
        buf.writeNbt(packet.item);
    }

    @SuppressWarnings("Convert2Lambda")
    public static void onMessage(PacketItemEnterPipe message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(new Runnable() {
            @Override
            public void run() {
                var mc = Minecraft.getInstance();
                if (mc.level == null)
                    return;
                var item = IPipeItem.load(message.item);
                var pipe = Utility.getBlockEntity(PipeBlockEntity.class, mc.level, message.tilePos);
                if (pipe != null)
                    pipe.getItems().add(item);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
