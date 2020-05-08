package de.ellpeck.prettypipes.network;

public class NetworkLock {

    public final NetworkLocation location;
    public final int slot;
    public final int amount;

    public NetworkLock(NetworkLocation location, int slot, int amount) {
        this.location = location;
        this.slot = slot;
        this.amount = amount;
    }
}
