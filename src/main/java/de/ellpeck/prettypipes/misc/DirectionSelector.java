package de.ellpeck.prettypipes.misc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.packets.PacketButton;
import de.ellpeck.prettypipes.pipe.PipeBlockEntity;
import joptsimple.internal.Strings;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.gui.widget.ExtendedButton;
import org.apache.commons.lang3.ArrayUtils;

import java.util.List;

public class DirectionSelector {

    private static final Direction[] ALL = ArrayUtils.addAll(Direction.values(), (Direction) null);

    // null means old behavior, which is all directions
    private Direction direction;
    private boolean modified;

    private final ItemStack stack;
    private final PipeBlockEntity pipe;

    public DirectionSelector(ItemStack stack, PipeBlockEntity pipe) {
        this.stack = stack;
        this.pipe = pipe;
        this.load();
    }

    @OnlyIn(Dist.CLIENT)
    public AbstractWidget getButton(int x, int y) {
        return new ExtendedButton(x, y, 100, 20, Component.translatable("info." + PrettyPipes.ID + ".populate"), button ->
            PacketButton.sendAndExecute(this.pipe.getBlockPos(), PacketButton.ButtonResult.DIRECTION_SELECTOR, List.of())) {
            @Override
            public Component getMessage() {
                var pipe = DirectionSelector.this.pipe;
                var dir = DirectionSelector.this.direction;
                var msg = Component.translatable("dir." + PrettyPipes.ID + "." + (dir != null ? dir.getName() : "all"));
                if (dir != null) {
                    var blockName = pipe.getItemHandler(dir) != null ? pipe.getLevel().getBlockState(pipe.getBlockPos().relative(dir)).getBlock().getName() : null;
                    if (blockName != null)
                        msg = msg.append(" (").append(blockName).append(")");
                }
                return msg;
            }
        };
    }

    public void onButtonPacket() {
        var dir = this.direction;
        do {
            dir = DirectionSelector.ALL[(ArrayUtils.indexOf(DirectionSelector.ALL, dir) + 1) % DirectionSelector.ALL.length];
        } while (!this.isDirectionValid(dir));
        if (this.direction != dir) {
            this.direction = dir;
            this.modified = true;
        }
    }

    public void save() {
        if (!this.modified)
            return;
        this.modified = false;
        this.stack.set(Data.TYPE, new Data(this.direction != null ? this.direction.getName() : ""));
    }

    public void load() {
        var data = this.stack.get(Data.TYPE);
        if (data != null)
            this.direction = !Strings.isNullOrEmpty(data.direction) ? Direction.byName(data.direction) : null;
    }

    public Direction[] directions() {
        return this.direction != null ? new Direction[]{this.direction} : Direction.values();
    }

    public boolean has(Direction dir) {
        return this.direction == null || this.direction == dir;
    }

    private boolean isDirectionValid(Direction dir) {
        if (dir == null)
            return true;
        if (this.pipe.getItemHandler(dir) == null)
            return false;
        return this.pipe.streamModules()
            .filter(p -> p.getLeft() != this.stack)
            .map(p -> p.getRight().getDirectionSelector(p.getLeft(), this.pipe))
            .noneMatch(p -> p != null && p.direction == dir);
    }

    public interface IDirectionContainer {

        DirectionSelector getSelector();

    }

    public record Data(String direction) {

        public static final Codec<DirectionSelector.Data> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("direction").forGetter(f -> f.direction)
        ).apply(i, DirectionSelector.Data::new));
        public static final DataComponentType<DirectionSelector.Data> TYPE = DataComponentType.<DirectionSelector.Data>builder().persistent(DirectionSelector.Data.CODEC).cacheEncoding().build();

    }

}
