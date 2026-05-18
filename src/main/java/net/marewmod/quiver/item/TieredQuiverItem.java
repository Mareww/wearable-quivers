package net.marewmod.quiver.item;

public class TieredQuiverItem extends QuiverItem {

    private final int capacity;

    public TieredQuiverItem(Settings settings, int capacity) {
        super(settings);
        this.capacity = capacity;
    }

    @Override
    public int capacity() {
        return capacity;
    }
}
