package com.ducishere.livewallpaperelaina.client.gui;

import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {
    private final FramePlayer elainaWallpaper = new FramePlayer("textures/frames/elaina", 578, 30);

    @Inject(method = "render", at = @At("HEAD"))
    private void renderCustomWallpaper(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        elainaWallpaper.render(context, context.getScaledWindowWidth(), context.getScaledWindowHeight());
    }
}
