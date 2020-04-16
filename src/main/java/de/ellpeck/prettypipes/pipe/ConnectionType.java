package de.ellpeck.prettypipes.pipe;

import net.minecraft.util.IStringSerializable;

import java.util.Locale;

public enum ConnectionType implements IStringSerializable {
    CONNECTED(true),
    DISCONNECTED(false),
    BLOCKED(false),
    LEGS(false);

    private final String name;
    private final boolean isConnected;

    ConnectionType(boolean isConnected) {
        this.name = this.name().toLowerCase(Locale.ROOT);
        this.isConnected = isConnected;
    }

    public boolean isConnected() {
        return this.isConnected;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
