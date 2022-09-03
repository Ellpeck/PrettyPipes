package de.ellpeck.prettypipes.items;

public enum ModuleTier {

    LOW,
    MEDIUM,
    HIGH;

    public final <T> T forTier(T low, T medium, T high) {
        return switch (this) {
            case LOW -> low;
            case MEDIUM -> medium;
            case HIGH -> high;
        };
    }
}
