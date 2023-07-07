package de.ellpeck.prettypipes.packets;

import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.terminal.ItemTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketRequest {

    private BlockPos pos;
    private ItemStack stack;
    private int nbtHash;
    private int amount;

    public PacketRequest(BlockPos pos, ItemStack stack, int amount) {
        this.pos = pos;
        this.stack = stack;
        if (stack.hasTag())
            this.nbtHash = stack.getTag().hashCode();
        this.amount = amount;
    }

    private PacketRequest() {

    }

    public static PacketRequest fromBytes(FriendlyByteBuf buf) {
        var packet = new PacketRequest();
        packet.pos = buf.readBlockPos();
        packet.stack = buf.readItem();
        packet.nbtHash = buf.readVarInt();
        packet.amount = buf.readVarInt();
        return packet;
    }

    public static void toBytes(PacketRequest packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeItem(packet.stack);
        buf.writeVarInt(packet.nbtHash);
        buf.writeVarInt(packet.amount);
    }

    @SuppressWarnings("Convert2Lambda")
    public static void onMessage(PacketRequest message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(new Runnable() {
            @Override
            public void run() {
                Player player = ctx.get().getSender();
                var tile = Utility.getBlockEntity(ItemTerminalBlockEntity.class, player.level(), message.pos);
                message.stack.setCount(message.amount);
                tile.requestItem(player, message.stack, message.nbtHash);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
