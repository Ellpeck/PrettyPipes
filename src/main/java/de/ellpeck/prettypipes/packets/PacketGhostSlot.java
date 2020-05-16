package de.ellpeck.prettypipes.packets;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.terminal.CraftingTerminalTileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PacketGhostSlot {

    private BlockPos pos;
    private ListMultimap<Integer, ItemStack> stacks;

    public PacketGhostSlot(BlockPos pos, ListMultimap<Integer, ItemStack> stacks) {
        this.pos = pos;
        this.stacks = stacks;
    }

    private PacketGhostSlot() {

    }

    public static PacketGhostSlot fromBytes(PacketBuffer buf) {
        PacketGhostSlot packet = new PacketGhostSlot();
        packet.pos = buf.readBlockPos();
        packet.stacks = ArrayListMultimap.create();
        for (int i = buf.readInt(); i > 0; i--)
            packet.stacks.put(buf.readInt(), buf.readItemStack());
        return packet;
    }

    public static void toBytes(PacketGhostSlot packet, PacketBuffer buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeInt(packet.stacks.size());
        for (Map.Entry<Integer, ItemStack> entry : packet.stacks.entries()) {
            buf.writeInt(entry.getKey());
            buf.writeItemStack(entry.getValue());
        }
    }

    @SuppressWarnings("Convert2Lambda")
    public static void onMessage(PacketGhostSlot message, Supplier<NetworkEvent.Context> ctx) {
        Consumer<PlayerEntity> doIt = p -> {
            CraftingTerminalTileEntity tile = Utility.getTileEntity(CraftingTerminalTileEntity.class, p.world, message.pos);
            if (tile != null)
                tile.setGhostItems(message.stacks);
        };

        // this whole thing is a dirty hack for allowing the same packet to be used
        // both client -> server and server -> client without any classloading issues
        PlayerEntity player = ctx.get().getSender();
        // are we on the client?
        if (player == null) {
            ctx.get().enqueueWork(new Runnable() {
                @Override
                public void run() {
                    doIt.accept(Minecraft.getInstance().player);
                }
            });
        } else {
            ctx.get().enqueueWork(new Runnable() {
                @Override
                public void run() {
                    doIt.accept(player);
                }
            });
        }

        ctx.get().setPacketHandled(true);
    }
}
