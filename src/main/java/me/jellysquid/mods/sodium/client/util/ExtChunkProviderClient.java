package me.jellysquid.mods.sodium.client.util;

public interface ExtChunkProviderClient {

    boolean needsTrackingUpdate();

    void setNeedsTrackingUpdate(boolean state);
}