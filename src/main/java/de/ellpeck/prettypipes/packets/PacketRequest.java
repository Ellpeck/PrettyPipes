package de.ellpeck.prettypipes.packets;

import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.terminal.ItemTerminalTileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketRequest {

    private BlockPos pos;
    private ItemStack stack;
    private int nbtHash;
    private int amount;

    public PacketRequest(BlockPos pos, ItemStack stack, int amount) {
        this.pos = pos;
        this.stack = stack;
        this.nbtHash = stack.getTag().hashCode();
        this.amount = amount;
    }

    private PacketRequest() {

    }

    public static PacketRequest fromBytes(PacketBuffer buf) {
        PacketRequest packet = new PacketRequest();
        packet.pos = buf.readBlockPos();
        packet.stack = buf.readItemStack();
        packet.nbtHash = buf.readVarInt();
        packet.amount = buf.readVarInt();
        return packet;
    }

    public static void toBytes(PacketRequest packet, PacketBuffer buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeItemStack(packet.stack);
        buf.writeVarInt(packet.nbtHash);
        buf.writeVarInt(packet.amount);
    }

    @SuppressWarnings("Convert2Lambda")
    public static void onMessage(PacketRequest message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(new Runnable() {
            @Override
            public void run() {
                PlayerEntity player = ctx.get().getSender();
                ItemTerminalTileEntity tile = Utility.getTileEntity(ItemTerminalTileEntity.class, player.world, message.pos);
                message.stack.setCount(message.amount);
                tile.requestItem(player, message.stack, message.nbtHash);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
