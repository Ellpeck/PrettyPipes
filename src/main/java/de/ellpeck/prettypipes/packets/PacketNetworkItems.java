package de.ellpeck.prettypipes.packets;

import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.network.PipeItem;
import de.ellpeck.prettypipes.pipe.PipeTileEntity;
import de.ellpeck.prettypipes.terminal.containers.ItemTerminalContainer;
import de.ellpeck.prettypipes.terminal.containers.ItemTerminalGui;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PacketNetworkItems {

    private List<ItemStack> items;

    public PacketNetworkItems(List<ItemStack> items) {
        this.items = items;
    }

    private PacketNetworkItems() {

    }

    public static PacketNetworkItems fromBytes(PacketBuffer buf) {
        PacketNetworkItems client = new PacketNetworkItems();
        client.items = new ArrayList<>();
        for (int i = buf.readVarInt(); i > 0; i--) {
            ItemStack stack = buf.readItemStack();
            stack.setCount(buf.readVarInt());
            client.items.add(stack);
        }
        return client;
    }

    public static void toBytes(PacketNetworkItems packet, PacketBuffer buf) {
        buf.writeVarInt(packet.items.size());
        for (ItemStack stack : packet.items) {
            buf.writeItemStack(stack);
            buf.writeVarInt(stack.getCount());
        }
    }

    @SuppressWarnings("Convert2Lambda")
    public static void onMessage(PacketNetworkItems message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(new Runnable() {
            @Override
            public void run() {
                Minecraft mc = Minecraft.getInstance();
                if (mc.currentScreen instanceof ItemTerminalGui)
                    ((ItemTerminalGui) mc.currentScreen).updateItemList(message.items);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
