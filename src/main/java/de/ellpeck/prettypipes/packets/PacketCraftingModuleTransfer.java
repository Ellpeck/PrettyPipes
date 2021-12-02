package de.ellpeck.prettypipes.packets;

import de.ellpeck.prettypipes.pipe.modules.craft.CraftingModuleContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

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

    public static PacketCraftingModuleTransfer fromBytes(PacketBuffer buf) {
        PacketCraftingModuleTransfer packet = new PacketCraftingModuleTransfer();
        packet.inputs = new ArrayList<>();
        for (int i = buf.readInt(); i > 0; i--)
            packet.inputs.add(buf.readItemStack());
        packet.outputs = new ArrayList<>();
        for (int i = buf.readInt(); i > 0; i--)
            packet.outputs.add(buf.readItemStack());
        return packet;
    }

    public static void toBytes(PacketCraftingModuleTransfer packet, PacketBuffer buf) {
        buf.writeInt(packet.inputs.size());
        for (ItemStack stack : packet.inputs)
            buf.writeItemStack(stack);
        buf.writeInt(packet.outputs.size());
        for (ItemStack stack : packet.outputs)
            buf.writeItemStack(stack);
    }

    @SuppressWarnings("Convert2Lambda")
    public static void onMessage(PacketCraftingModuleTransfer message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(new Runnable() {
            @Override
            public void run() {
                PlayerEntity player = ctx.get().getSender();
                if (player.openContainer instanceof CraftingModuleContainer) {
                    CraftingModuleContainer container = (CraftingModuleContainer) player.openContainer;
                    copy(container.input, message.inputs);
                    copy(container.output, message.outputs);
                    container.modified = true;
                    container.detectAndSendChanges();
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void copy(ItemStackHandler container, List<ItemStack> contents) {
        for (int i = 0; i < container.getSlots(); i++)
            container.setStackInSlot(i, ItemStack.EMPTY);
        for (ItemStack stack : contents)
            ItemHandlerHelper.insertItem(container, stack, false);
    }
}
