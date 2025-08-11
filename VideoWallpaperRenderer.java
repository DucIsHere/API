package com.ducishere.livewallpaperelaina.client.render;

import net.minecraft.client.gui.DrawContext;

public class VideoWallpaperRenderer {
    private static FramePlayer player;

    public static void init() {
        if (player != null) return;
        // framesPathPrefix corresponds to assets/.../textures/<framesPathPrefix>/frame_0001.png
        // We used "frames/elaina" in examples
        player = new FramePlayer("frames/elaina", 578, 30, 6); // fps 30, cache 6 frames
        player.start();
    }

    public static void render(DrawContext context, int width, int height) {
        if (player == null) return;
        player.render(context, width, height);
    }

    public static void shutdown() {
        if (player != null) {
            player.stop();
            player = null;
        }
    }
}
