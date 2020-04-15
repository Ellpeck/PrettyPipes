package de.ellpeck.prettypipes.items;

public enum ModuleTier {

    LOW,
    MEDIUM,
    HIGH;

    public final <T> T forTier(T low, T medium, T high) {
        switch (this) {
            case LOW:
                return low;
            case MEDIUM:
                return medium;
            case HIGH:
                return high;
            default:
                throw new RuntimeException();
        }
    }
}
