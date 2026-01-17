package de.ellpeck.prettypipes.packets;

import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.items.IModule;
import de.ellpeck.prettypipes.misc.FilterSlot;
import de.ellpeck.prettypipes.pipe.PipeBlockEntity;
import mezz.jei.api.helpers.ICodecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;

public record PacketFilterSlot(int slotIndex, ItemStack stack) implements CustomPacketPayload {
    public static final Type<PacketFilterSlot> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(PrettyPipes.ID, "ghost_filter_slot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketFilterSlot> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, PacketFilterSlot::slotIndex,
            ItemStack.STREAM_CODEC, PacketFilterSlot::stack,
            PacketFilterSlot::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PacketFilterSlot.TYPE;
    }
    public static void onMessage(PacketFilterSlot message, IPayloadContext ctx) {
        var player = ctx.player();
        if(player.containerMenu.slots.get(message.slotIndex()) instanceof FilterSlot filterSlot){
            filterSlot.slotClick(player.containerMenu, message.stack());
        }

    }
}
