package com.ducishere.livewallpaperelaina.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.util.concurrent.*;

public class FramePlayer {
    private final String framesPathPrefix; // "frames/elaina"
    private final int totalFrames; // 578
    private final int fps;
    private final int cacheSize;
    private final MinecraftClient client;
    private final TextureManager textureManager;

    private final ScheduledExecutorService loaderExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "FramePlayer-Loader");
        t.setDaemon(true);
        return t;
    });

    // Holds already registered texture Identifiers (on render thread)
    private final ConcurrentHashMap<Integer, Identifier> registered = new ConcurrentHashMap<>();
    // Holds NativeImage objects loaded by background thread waiting to be registered
    private final ConcurrentHashMap<Integer, NativeImage> pendingNative = new ConcurrentHashMap<>();
    private volatile boolean running = false;

    private int currentFrameIndex = 1;
    private long lastFrameTime = 0L;

    public FramePlayer(String framesPathPrefix, int totalFrames, int fps, int cacheSize) {
        this.framesPathPrefix = framesPathPrefix;
        this.totalFrames = totalFrames;
        this.fps = fps;
        this.cacheSize = Math.max(2, cacheSize);
        this.client = MinecraftClient.getInstance();
        this.textureManager = client.getTextureManager();
    }

    public void start() {
        if (running) return;
        running = true;
        // initial prefetch
        prefetchAround(currentFrameIndex);
        // schedule prefetch task: try to keep cache full
        loaderExecutor.scheduleWithFixedDelay(this::maintainCache, 100, 100, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        running = false;
        loaderExecutor.shutdownNow();
        // cleanup textures on client thread
        client.execute(() -> {
            registered.forEach((idx, id) -> textureManager.destroyTexture(id));
            registered.clear();
            pendingNative.forEach((i, n) -> n.close());
            pendingNative.clear();
        });
    }

    private void maintainCache() {
        try {
            if (!running) return;
            // ensure we have next N frames loaded or pending
            for (int i = 0; i < cacheSize; i++) {
                int idx = wrapIndex(currentFrameIndex + i);
                if (registered.containsKey(idx) || pendingNative.containsKey(idx)) continue;
                loadNativeInBackground(idx);
            }
            // evict far away frames
            registered.keySet().removeIf(idx -> Math.abs(distanceFromCurrent(idx)) > cacheSize * 2);
            pendingNative.keySet().removeIf(idx -> Math.abs(distanceFromCurrent(idx)) > cacheSize * 2);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private int distanceFromCurrent(int idx) {
        int diff = idx - currentFrameIndex;
        // handle wrap-around distances properly
        if (Math.abs(diff) > totalFrames / 2) {
            if (diff > 0) diff -= totalFrames;
            else diff += totalFrames;
        }
        return diff;
    }

    private int wrapIndex(int i) {
        while (i > totalFrames) i -= totalFrames;
        while (i < 1) i += totalFrames;
        return i;
    }

    private void prefetchAround(int center) {
        for (int i = 0; i < cacheSize; i++) {
            int idx = wrapIndex(center + i);
            loadNativeInBackground(idx);
        }
    }

    private void loadNativeInBackground(int frameIndex) {
        // avoid duplicate
        if (pendingNative.containsKey(frameIndex) || registered.containsKey(frameIndex)) return;

        loaderExecutor.execute(() -> {
            try {
                String name = String.format("%s/frame_%04d.png", framesPathPrefix, frameIndex);
                Identifier resId = new Identifier("livewallpaperelaina", "textures/" + name);
                // resource path inside jar: assets/livewallpaperelaina/textures/frames/elaina/...
                InputStream input = MinecraftClient.getInstance().getResourceManager().getResource(resId).getInputStream();
                NativeImage img = NativeImage.read(input);
                input.close();
                // place into pending for registration on render thread
                pendingNative.put(frameIndex, img);
                // schedule registration on client thread ASAP
                client.execute(() -> registerPending(frameIndex));
            } catch (Exception e) {
                // if not found or error, skip
                e.printStackTrace();
            }
        });
    }

    private void registerPending(int frameIndex) {
        if (!pendingNative.containsKey(frameIndex)) return;
        try {
            NativeImage img = pendingNative.remove(frameIndex);
            if (img == null) return;
            Identifier texId = new Identifier("livewallpaperelaina", "dynamic/frame_" + frameIndex);
            NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
            textureManager.registerTexture(texId, tex);
            registered.put(frameIndex, texId);
            // note: don't close img here because the NativeImageBackedTexture takes ownership
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void render(net.minecraft.client.gui.DrawContext context, int width, int height) {
        if (!running) return;
        long now = System.currentTimeMillis();
        long frameDuration = 1000L / Math.max(1, fps);
        if (now - lastFrameTime >= frameDuration) {
            currentFrameIndex = wrapIndex(currentFrameIndex + 1);
            lastFrameTime = now;
            // ensure next frames prefetch
            prefetchAround(currentFrameIndex);
        }

        // get best available texture for currentFrameIndex
        Identifier tex = registered.get(currentFrameIndex);
        if (tex == null) {
            // fallback: try current-1 or any available nearby
            for (int offset = 0; offset < cacheSize; offset++) {
                int idx = wrapIndex(currentFrameIndex - offset);
                tex = registered.get(idx);
                if (tex != null) break;
            }
        }

        if (tex != null) {
            // draw full screen
            RenderSystem.enableBlend();
            context.drawTexture(tex, 0, 0, 0, 0, width, height, width, height);
        } else {
            // nothing loaded yet; do nothing or draw a placeholder
        }
    }
}
