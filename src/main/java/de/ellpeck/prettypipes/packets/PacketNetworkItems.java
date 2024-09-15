package de.ellpeck.prettypipes.packets;

import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.terminal.containers.ItemTerminalGui;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record PacketNetworkItems(List<ItemStack> items, List<ItemStack> craftables, List<ItemStack> currentlyCrafting) implements CustomPacketPayload {

    public static final Type<PacketNetworkItems> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(PrettyPipes.ID, "network_items"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketNetworkItems> CODEC = StreamCodec.composite(
        ByteBufCodecs.collection(ArrayList::new, ItemStack.STREAM_CODEC), PacketNetworkItems::items,
        ByteBufCodecs.collection(ArrayList::new, ItemStack.STREAM_CODEC), PacketNetworkItems::craftables,
        ByteBufCodecs.collection(ArrayList::new, ItemStack.STREAM_CODEC), PacketNetworkItems::currentlyCrafting,
        PacketNetworkItems::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PacketNetworkItems.TYPE;
    }

    public static void onMessage(PacketNetworkItems message, IPayloadContext ctx) {
        var mc = Minecraft.getInstance();
        if (mc.screen instanceof ItemTerminalGui terminal)
            terminal.updateItemList(message.items, message.craftables, message.currentlyCrafting);
    }

}
