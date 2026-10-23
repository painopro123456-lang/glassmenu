package dev.glassmenu.modules;

import com.mojang.blaze3d.platform.NativeImage;
import dev.glassmenu.api.Module;
import dev.glassmenu.api.NumberSetting;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Shows a replaceable image for a few seconds, then fades it away. */
public final class JumpscareModule extends Module {
    private static final Identifier TEXTURE_ID = Identifier.fromNamespaceAndPath("glassmenu", "jumpscare_custom");
    private static final String RESOURCE = "/assets/glassmenu/textures/gui/jumpscare.png";
    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    private final NumberSetting visibleSeconds = number("visible_seconds", "Visible time", 1, 10, 1, 5);
    private final NumberSetting fadeSeconds = number("fade_seconds", "Fade time", 0, 5, 0.25, 1);
    private long startedAt;
    private int imageWidth;
    private int imageHeight;
    private boolean imageReady;

    public JumpscareModule() {
        super("jumpscare", "Jumpscare", "Shows your custom image; replace config/glassmenu/jumpscare.png to choose it.", false, Module.Category.RENDER);
        hudOpacity.set(100);
    }

    @Override
    public boolean isFullScreenOverlay() { return true; }

    @Override
    public boolean persistsEnabledState() { return false; }

    @Override
    public String hudLine(Minecraft mc) { return null; }

    @Override
    public int hudWidth(Font font, Minecraft mc, String displayText) { return 0; }

    @Override
    public int hudHeight(Font font, Minecraft mc, String displayText) { return 0; }

    @Override
    public void onEnable() {
        startedAt = System.nanoTime();
        imageReady = loadImage();
    }

    @Override
    public void onTick(Minecraft mc) {
        double duration = visibleSeconds.get() + fadeSeconds.get();
        if ((System.nanoTime() - startedAt) / (double) NANOS_PER_SECOND >= duration) {
            setEnabled(false);
        }
    }

    @Override
    public void renderHud(GuiGraphicsExtractor g, Font font, Minecraft mc, int screenW, int screenH, float dt) {
        if (!imageReady || screenW <= 0 || screenH <= 0) return;

        double elapsed = (System.nanoTime() - startedAt) / (double) NANOS_PER_SECOND;
        double fade = fadeSeconds.get();
        double fadeProgress = fade <= 0 || elapsed <= visibleSeconds.get()
                ? 1.0
                : Math.max(0.0, 1.0 - (elapsed - visibleSeconds.get()) / fade);
        double opacity = hudOpacity.get() / 100.0;
        int imageAlpha = (int) Math.round(255.0 * fadeProgress * opacity);
        if (imageAlpha <= 0) return;

        int backdropAlpha = (int) Math.round(96.0 * fadeProgress * opacity);
        g.fill(0, 0, screenW, screenH, backdropAlpha << 24);

        double scale = Math.min(screenW * 0.94 / imageWidth, screenH * 0.94 / imageHeight);
        int drawW = Math.max(1, (int) Math.round(imageWidth * scale));
        int drawH = Math.max(1, (int) Math.round(imageHeight * scale));
        int x = (screenW - drawW) / 2;
        int y = (screenH - drawH) / 2;
        int tint = (imageAlpha << 24) | 0x00FFFFFF;

        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE_ID, x, y, 0.0f, 0.0f,
                drawW, drawH, imageWidth, imageHeight, imageWidth, imageHeight, tint);
    }

    private boolean loadImage() {
        Path imagePath = FabricLoader.getInstance().getConfigDir()
                .resolve("glassmenu").resolve("jumpscare.png");
        try {
            Files.createDirectories(imagePath.getParent());
            if (Files.notExists(imagePath)) {
                try (InputStream bundled = JumpscareModule.class.getResourceAsStream(RESOURCE)) {
                    if (bundled == null) throw new IllegalStateException("Bundled jumpscare image is missing.");
                    Files.copy(bundled, imagePath);
                }
            }

            NativeImage image;
            try (InputStream input = Files.newInputStream(imagePath)) {
                image = NativeImage.read(input);
            }
            imageWidth = image.getWidth();
            imageHeight = image.getHeight();
            if (imageWidth <= 0 || imageHeight <= 0 || imageWidth > 4096 || imageHeight > 4096
                    || (long) imageWidth * imageHeight > 16_777_216L) {
                image.close();
                throw new IllegalArgumentException("Image must be no larger than 4096 by 4096 pixels.");
            }

            DynamicTexture texture = new DynamicTexture(() -> "Moon Menu jumpscare image", image);
            texture.upload();
            Minecraft.getInstance().getTextureManager().register(TEXTURE_ID, texture);
            return true;
        } catch (Exception e) {
            System.err.println("[glassmenu] Could not load jumpscare image from " + imagePath + ": " + e);
            return false;
        }
    }
}
