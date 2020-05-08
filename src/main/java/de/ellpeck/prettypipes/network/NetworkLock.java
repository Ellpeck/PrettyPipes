package de.ellpeck.prettypipes.network;

import java.util.Objects;

public class NetworkLock {

    public final NetworkLocation location;
    public final int slot;
    public final int amount;

    public NetworkLock(NetworkLocation location, int slot, int amount) {
        this.location = location;
        this.slot = slot;
        this.amount = amount;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof NetworkLock) {
            NetworkLock that = (NetworkLock) o;
            return this.slot == that.slot && this.amount == that.amount && this.location.equals(that.location);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.location, this.slot, this.amount);
    }
}
