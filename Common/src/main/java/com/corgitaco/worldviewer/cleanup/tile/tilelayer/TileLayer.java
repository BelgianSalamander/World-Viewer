package com.corgitaco.worldviewer.cleanup.tile.tilelayer;

import com.corgitaco.worldviewer.cleanup.WorldScreenv2;
import com.corgitaco.worldviewer.cleanup.storage.DataTileManager;
import com.corgitaco.worldviewer.cleanup.tile.TileV2;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class TileLayer {

    public static final Map<String, Factory> FACTORY_REGISTRY = Util.make(new LinkedHashMap<>(), map -> {
        map.put("heights", HeightsLayer::new);
        map.put("biomes", BiomeLayer::new);
        map.put("slime_chunks", SlimeChunkLayer::new);
        map.put("structures", StructuresLayer::new);
    });

    private final int tileWorldX;
    private final int tileWorldZ;

    public TileLayer(DataTileManager dataTileManager, int y, int tileWorldX, int tileWorldZ, int size, int sampleResolution, WorldScreenv2 screen) {
        this.tileWorldX = tileWorldX;
        this.tileWorldZ = tileWorldZ;
    }

    @Nullable
    public Component toolTip(double mouseX, double mouseY, double mouseWorldX, double mouseWorldZ) {
        return null;
    }

    public void afterTilesRender(PoseStack stack, double mouseX, double mouseY, double mouseWorldX, double mouseWorldZ) {
    }


    @Nullable
    public DynamicTexture getImage() {
        return null;
    }

    public void close() {
    }

    public int getTileWorldX() {
        return tileWorldX;
    }

    public int getTileWorldZ() {
        return tileWorldZ;
    }

    public boolean canRender(TileV2 tileV2, Collection<String> currentlyRendering) {
        return true;
    }

    @FunctionalInterface
    public interface Factory {

        TileLayer make(DataTileManager tileManager, int scrollWorldY, int tileWorldX, int tileWorldZ, int size, int sampleResolution, WorldScreenv2 screen);
    }
}


