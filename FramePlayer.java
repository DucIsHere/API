package com.ducishere.livewallpaperelaina.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public class FramePlayer {
    private final String path;
    private final int totalFrames;
    private final int fps;
    private int currentFrame;
    private long lastFrameTime;

    public FramePlayer(String path, int totalFrames, int fps) {
        this.path = path;
        this.totalFrames = totalFrames;
        this.fps = fps;
    }

    public void render(DrawContext context, int width, int height) {
        long now = System.currentTimeMillis();
        if (now - lastFrameTime >= 1000 / fps) {
            currentFrame = (currentFrame + 1) % totalFrames;
            lastFrameTime = now;
        }
        Identifier frame = new Identifier("livewallpaperelaina",
                "textures/frames/elaina/frame_" + String.format("%04d", currentFrame + 1) + ".png");
        context.drawTexture(frame, 0, 0, 0, 0, width, height, width, height);
    }
}
