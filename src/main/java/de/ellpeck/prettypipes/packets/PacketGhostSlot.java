package de.ellpeck.prettypipes.packets;

import com.google.common.collect.Streams;
import com.mojang.datafixers.util.Pair;
import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.terminal.CraftingTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public record PacketGhostSlot(BlockPos pos, List<Entry> stacks) implements CustomPacketPayload {

    public static final Type<PacketGhostSlot> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(PrettyPipes.ID, "ghost_slot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketGhostSlot> CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, PacketGhostSlot::pos,
        ByteBufCodecs.collection(ArrayList::new, Entry.CODEC), PacketGhostSlot::stacks,
        PacketGhostSlot::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PacketGhostSlot.TYPE;
    }

    public static void onMessage(PacketGhostSlot message, IPayloadContext ctx) {
        var player = ctx.player();
        var tile = Utility.getBlockEntity(CraftingTerminalBlockEntity.class, player.level(), message.pos);
        if (tile != null)
            tile.setGhostItems(message.stacks);
    }

    public record Entry(List<ItemStack> stacks, TagKey<Item> tag) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_LIST_STREAM_CODEC, Entry::stacks,
            ByteBufCodecs.fromCodec(TagKey.codec(Registries.ITEM)), Entry::tag,
            Entry::new);

        public static Entry fromStacks(Level level, List<ItemStack> stacks) {
            var tag = Entry.getTagForStacks(level, stacks);
            if (tag != null) {
                return new Entry(null, tag);
            } else {
                return new Entry(stacks, null);
            }
        }

        public List<ItemStack> getStacks(Level level) {
            if (this.stacks != null)
                return this.stacks;
            return Streams.stream(level.registryAccess().registry(Registries.ITEM).orElseThrow().getTagOrEmpty(this.tag).iterator())
                .filter(h -> h.value() != null & h.value() != Items.AIR)
                .map(h -> new ItemStack(h.value())).collect(Collectors.toList());
        }

        private static TagKey<Item> getTagForStacks(Level level, List<ItemStack> stacks) {
            return level.registryAccess().registry(Registries.ITEM).orElseThrow().getTags().filter(e -> {
                var tag = e.getSecond();
                if (tag.size() != stacks.size())
                    return false;
                for (var i = 0; i < tag.size(); i++) {
                    if (stacks.get(i).getItem() != tag.get(i).value())
                        return false;
                }
                return true;
            }).map(Pair::getFirst).findFirst().orElse(null);
        }

    }

}
