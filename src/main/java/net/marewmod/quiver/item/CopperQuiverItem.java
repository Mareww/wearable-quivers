package net.marewmod.quiver.item;

public class CopperQuiverItem extends QuiverItem {

    public static final int CAPACITY = 128;

    public CopperQuiverItem(Settings settings) {
        super(settings);
    }

    @Override
    public int capacity() {
        return CAPACITY;
    }
}
