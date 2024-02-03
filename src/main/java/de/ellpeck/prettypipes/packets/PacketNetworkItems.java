package de.ellpeck.prettypipes.packets;

import de.ellpeck.prettypipes.terminal.containers.ItemTerminalGui;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.NetworkEvent;

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

    public static PacketNetworkItems fromBytes(FriendlyByteBuf buf) {
        var client = new PacketNetworkItems();
        client.items = new ArrayList<>();
        for (var i = buf.readVarInt(); i > 0; i--) {
            var stack = buf.readItem();
            stack.setCount(buf.readVarInt());
            client.items.add(stack);
        }
        client.craftables = new ArrayList<>();
        for (var i = buf.readVarInt(); i > 0; i--)
            client.craftables.add(buf.readItem());
        client.currentlyCrafting = new ArrayList<>();
        for (var i = buf.readVarInt(); i > 0; i--)
            client.currentlyCrafting.add(buf.readItem());
        return client;
    }

    public static void toBytes(PacketNetworkItems packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.items.size());
        for (var stack : packet.items) {
            var copy = stack.copy();
            copy.setCount(1);
            buf.writeItem(copy);
            buf.writeVarInt(stack.getCount());
        }
        buf.writeVarInt(packet.craftables.size());
        for (var stack : packet.craftables)
            buf.writeItem(stack);
        buf.writeVarInt(packet.currentlyCrafting.size());
        for (var stack : packet.currentlyCrafting)
            buf.writeItem(stack);
    }

    @SuppressWarnings("Convert2Lambda")
    public static void onMessage(PacketNetworkItems message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(new Runnable() {
            @Override
            public void run() {
                var mc = Minecraft.getInstance();
                if (mc.screen instanceof ItemTerminalGui terminal)
                    terminal.updateItemList(message.items, message.craftables, message.currentlyCrafting);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
