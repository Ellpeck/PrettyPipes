package de.ellpeck.prettypipes.packets;

import com.google.common.collect.Streams;
import com.mojang.datafixers.util.Pair;
import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.terminal.CraftingTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PacketGhostSlot implements CustomPacketPayload {

    public static final ResourceLocation ID = new ResourceLocation(PrettyPipes.ID, "ghost_slot");

    private final BlockPos pos;
    private final List<Entry> stacks;

    public PacketGhostSlot(BlockPos pos, List<Entry> stacks) {
        this.pos = pos;
        this.stacks = stacks;
    }

    public PacketGhostSlot(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.stacks = new ArrayList<>();
        for (var i = buf.readInt(); i > 0; i--)
            this.stacks.add(new Entry(buf));
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeInt(this.stacks.size());
        for (var entry : this.stacks)
            entry.write(buf);
    }

    @Override
    public ResourceLocation id() {
        return PacketGhostSlot.ID;
    }

    public static void onMessage(PacketGhostSlot message, PlayPayloadContext ctx) {
        ctx.workHandler().execute(() -> {
            var player = ctx.player().orElseThrow();
            var tile = Utility.getBlockEntity(CraftingTerminalBlockEntity.class, player.level(), message.pos);
            if (tile != null)
                tile.setGhostItems(message.stacks);
        });
    }

    public static class Entry {

        private final List<ItemStack> stacks;
        private final TagKey<Item> tag;

        public Entry(Level level, List<ItemStack> stacks) {
            var tag = Entry.getTagForStacks(level, stacks);
            if (tag != null) {
                this.stacks = null;
                this.tag = tag;
            } else {
                this.stacks = stacks;
                this.tag = null;
            }
        }

        public Entry(FriendlyByteBuf buf) {
            if (buf.readBoolean()) {
                this.tag = null;
                this.stacks = new ArrayList<>();
                for (var i = buf.readInt(); i > 0; i--)
                    this.stacks.add(buf.readItem());
            } else {
                this.stacks = null;
                this.tag = TagKey.create(Registries.ITEM, new ResourceLocation(buf.readUtf()));
            }
        }

        public List<ItemStack> getStacks(Level level) {
            if (this.stacks != null)
                return this.stacks;
            return Streams.stream(level.registryAccess().registry(Registries.ITEM).orElseThrow().getTagOrEmpty(this.tag).iterator())
                    .filter(h -> h.value() != null & h.value() != Items.AIR)
                    .map(h -> new ItemStack(h.value())).collect(Collectors.toList());
        }

        public void write(FriendlyByteBuf buf) {
            if (this.stacks != null) {
                buf.writeBoolean(true);
                buf.writeInt(this.stacks.size());
                for (var stack : this.stacks)
                    buf.writeItem(stack);
            } else {
                buf.writeBoolean(false);
                buf.writeUtf(this.tag.location().toString());
            }
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
