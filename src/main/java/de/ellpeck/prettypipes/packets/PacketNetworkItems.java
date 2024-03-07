package de.ellpeck.prettypipes.packets;

import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.terminal.containers.ItemTerminalGui;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

import java.util.ArrayList;
import java.util.List;

public class PacketNetworkItems implements CustomPacketPayload {

    public static final ResourceLocation ID = new ResourceLocation(PrettyPipes.ID, "network_items");

    private final List<ItemStack> items;
    private final List<ItemStack> craftables;
    private final List<ItemStack> currentlyCrafting;

    public PacketNetworkItems(List<ItemStack> items, List<ItemStack> craftables, List<ItemStack> currentlyCrafting) {
        this.items = items;
        this.craftables = craftables;
        this.currentlyCrafting = currentlyCrafting;
    }

    public PacketNetworkItems(FriendlyByteBuf buf) {
        this.items = new ArrayList<>();
        for (var i = buf.readVarInt(); i > 0; i--) {
            var stack = buf.readItem();
            stack.setCount(buf.readVarInt());
            this.items.add(stack);
        }
        this.craftables = new ArrayList<>();
        for (var i = buf.readVarInt(); i > 0; i--)
            this.craftables.add(buf.readItem());
        this.currentlyCrafting = new ArrayList<>();
        for (var i = buf.readVarInt(); i > 0; i--)
            this.currentlyCrafting.add(buf.readItem());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.items.size());
        for (var stack : this.items) {
            var copy = stack.copy();
            copy.setCount(1);
            buf.writeItem(copy);
            buf.writeVarInt(stack.getCount());
        }
        buf.writeVarInt(this.craftables.size());
        for (var stack : this.craftables)
            buf.writeItem(stack);
        buf.writeVarInt(this.currentlyCrafting.size());
        for (var stack : this.currentlyCrafting)
            buf.writeItem(stack);
    }

    @Override
    public ResourceLocation id() {
        return PacketNetworkItems.ID;
    }

    @SuppressWarnings("Convert2Lambda")
    public static void onMessage(PacketNetworkItems message, PlayPayloadContext ctx) {
        ctx.workHandler().execute(new Runnable() {
            @Override
            public void run() {
                var mc = Minecraft.getInstance();
                if (mc.screen instanceof ItemTerminalGui terminal)
                    terminal.updateItemList(message.items, message.craftables, message.currentlyCrafting);
            }
        });
    }

}
