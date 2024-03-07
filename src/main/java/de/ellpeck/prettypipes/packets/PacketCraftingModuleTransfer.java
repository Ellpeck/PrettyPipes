package de.ellpeck.prettypipes.packets;

import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.pipe.modules.craft.CraftingModuleContainer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

import java.util.ArrayList;
import java.util.List;

public class PacketCraftingModuleTransfer implements CustomPacketPayload {

    public static final ResourceLocation ID = new ResourceLocation(PrettyPipes.ID, "crafting_module_transfer");

    private final List<ItemStack> inputs;
    private final List<ItemStack> outputs;

    public PacketCraftingModuleTransfer(List<ItemStack> inputs, List<ItemStack> outputs) {
        this.inputs = inputs;
        this.outputs = outputs;
    }

    public PacketCraftingModuleTransfer(FriendlyByteBuf buf) {
        this.inputs = new ArrayList<>();
        for (var i = buf.readInt(); i > 0; i--)
            this.inputs.add(buf.readItem());
        this.outputs = new ArrayList<>();
        for (var i = buf.readInt(); i > 0; i--)
            this.outputs.add(buf.readItem());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(this.inputs.size());
        for (var stack : this.inputs)
            buf.writeItem(stack);
        buf.writeInt(this.outputs.size());
        for (var stack : this.outputs)
            buf.writeItem(stack);
    }

    @Override
    public ResourceLocation id() {
        return PacketCraftingModuleTransfer.ID;
    }

    public static void onMessage(PacketCraftingModuleTransfer message, PlayPayloadContext ctx) {
        ctx.workHandler().execute(() -> {
            var player = ctx.player().orElseThrow();
            if (player.containerMenu instanceof CraftingModuleContainer container) {
                PacketCraftingModuleTransfer.copy(container.input, message.inputs);
                PacketCraftingModuleTransfer.copy(container.output, message.outputs);
                container.modified = true;
                container.broadcastChanges();
            }
        });
    }

    private static void copy(ItemStackHandler container, List<ItemStack> contents) {
        for (var i = 0; i < container.getSlots(); i++)
            container.setStackInSlot(i, ItemStack.EMPTY);
        for (var stack : contents)
            ItemHandlerHelper.insertItem(container, stack, false);
    }

}
