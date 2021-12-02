package de.ellpeck.prettypipes.packets;

import de.ellpeck.prettypipes.pipe.modules.craft.CraftingModuleContainer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PacketCraftingModuleTransfer {

    private List<ItemStack> inputs;
    private List<ItemStack> outputs;

    public PacketCraftingModuleTransfer(List<ItemStack> inputs, List<ItemStack> outputs) {
        this.inputs = inputs;
        this.outputs = outputs;
    }

    private PacketCraftingModuleTransfer() {

    }

    public static PacketCraftingModuleTransfer fromBytes(FriendlyByteBuf buf) {
        var packet = new PacketCraftingModuleTransfer();
        packet.inputs = new ArrayList<>();
        for (var i = buf.readInt(); i > 0; i--)
            packet.inputs.add(buf.readItem());
        packet.outputs = new ArrayList<>();
        for (var i = buf.readInt(); i > 0; i--)
            packet.outputs.add(buf.readItem());
        return packet;
    }

    public static void toBytes(PacketCraftingModuleTransfer packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.inputs.size());
        for (var stack : packet.inputs)
            buf.writeItem(stack);
        buf.writeInt(packet.outputs.size());
        for (var stack : packet.outputs)
            buf.writeItem(stack);
    }

    @SuppressWarnings("Convert2Lambda")
    public static void onMessage(PacketCraftingModuleTransfer message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(new Runnable() {
            @Override
            public void run() {
                Player player = ctx.get().getSender();
                if (player.containerMenu instanceof CraftingModuleContainer container) {
                    copy(container.input, message.inputs);
                    copy(container.output, message.outputs);
                    container.modified = true;
                    container.broadcastChanges();
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void copy(ItemStackHandler container, List<ItemStack> contents) {
        for (var i = 0; i < container.getSlots(); i++)
            container.setStackInSlot(i, ItemStack.EMPTY);
        for (var stack : contents)
            ItemHandlerHelper.insertItem(container, stack, false);
    }
}
