package de.ellpeck.prettypipes.packets;

import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.pipe.modules.craft.CraftingModuleContainer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record PacketCraftingModuleTransfer(List<ItemStack> inputs, List<ItemStack> outputs) implements CustomPacketPayload {

    public static final Type<PacketCraftingModuleTransfer> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(PrettyPipes.ID, "crafting_module_transfer"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketCraftingModuleTransfer> CODEC = StreamCodec.composite(
        ItemStack.LIST_STREAM_CODEC, PacketCraftingModuleTransfer::inputs,
        ItemStack.LIST_STREAM_CODEC, PacketCraftingModuleTransfer::outputs,
        PacketCraftingModuleTransfer::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PacketCraftingModuleTransfer.TYPE;
    }

    public static void onMessage(PacketCraftingModuleTransfer message, IPayloadContext ctx) {
        var player = ctx.player();
        if (player.containerMenu instanceof CraftingModuleContainer container) {
            PacketCraftingModuleTransfer.copy(container.input, message.inputs);
            PacketCraftingModuleTransfer.copy(container.output, message.outputs);
            container.modified = true;
            container.broadcastChanges();
        }
    }

    private static void copy(ItemStackHandler container, List<ItemStack> contents) {
        for (var i = 0; i < container.getSlots(); i++)
            container.setStackInSlot(i, ItemStack.EMPTY);
        for (var stack : contents)
            ItemHandlerHelper.insertItem(container, stack, false);
    }

}
