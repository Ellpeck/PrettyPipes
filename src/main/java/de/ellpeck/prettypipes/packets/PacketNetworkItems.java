package de.ellpeck.prettypipes.packets;

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
    private List<ItemStack> craftables;
    private List<ItemStack> currentlyCrafting;

    public PacketNetworkItems(List<ItemStack> items, List<ItemStack> craftables, List<ItemStack> currentlyCrafting) {
        this.items = items;
        this.craftables = craftables;
        this.currentlyCrafting = currentlyCrafting;
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
        client.craftables = new ArrayList<>();
        for (int i = buf.readVarInt(); i > 0; i--)
            client.craftables.add(buf.readItemStack());
        client.currentlyCrafting = new ArrayList<>();
        for (int i = buf.readVarInt(); i > 0; i--)
            client.currentlyCrafting.add(buf.readItemStack());
        return client;
    }

    public static void toBytes(PacketNetworkItems packet, PacketBuffer buf) {
        buf.writeVarInt(packet.items.size());
        for (ItemStack stack : packet.items) {
            ItemStack copy = stack.copy();
            copy.setCount(1);
            buf.writeItemStack(copy);
            buf.writeVarInt(stack.getCount());
        }
        buf.writeVarInt(packet.craftables.size());
        for (ItemStack stack : packet.craftables)
            buf.writeItemStack(stack);
        buf.writeVarInt(packet.currentlyCrafting.size());
        for (ItemStack stack : packet.currentlyCrafting)
            buf.writeItemStack(stack);
    }

    @SuppressWarnings("Convert2Lambda")
    public static void onMessage(PacketNetworkItems message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(new Runnable() {
            @Override
            public void run() {
                Minecraft mc = Minecraft.getInstance();
                if (mc.currentScreen instanceof ItemTerminalGui)
                    ((ItemTerminalGui) mc.currentScreen).updateItemList(message.items, message.craftables, message.currentlyCrafting);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
