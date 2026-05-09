package sweetie.nezi.client.ui.widget.overlay;

import dev.redstones.mediaplayerinfo.IMediaSession;
import dev.redstones.mediaplayerinfo.MediaInfo;
import dev.redstones.mediaplayerinfo.MediaPlayerInfo;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.client.TickEvent;
import sweetie.nezi.api.event.events.render.Render2DEvent;
import sweetie.nezi.api.module.setting.BooleanSetting;
import sweetie.nezi.api.utils.color.UIColors;
import sweetie.nezi.api.utils.math.MathUtil;
import sweetie.nezi.api.utils.render.RenderUtil;
import sweetie.nezi.api.utils.render.ScissorUtil;
import sweetie.nezi.api.utils.render.fonts.Font;
import sweetie.nezi.client.features.modules.render.InterfaceModule;
import sweetie.nezi.client.ui.widget.Widget;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class MusicInfoWidget extends Widget {
    private static final MediaInfo EMPTY_MEDIA = new MediaInfo("", "", new byte[0], 0L, 0L, false);

    private final ExecutorService mediaExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "sweetie-mediainfo");
        thread.setDaemon(true);
        return thread;
    });

    private final Identifier artworkId = Identifier.of("sweetie", "hud/music_artwork");
    private volatile MediaInfo mediaInfo = EMPTY_MEDIA;
    private volatile String currentOwner = "";
    private volatile boolean artworkRegistered;
    private volatile int lastArtworkHash;

    private long lastMediaEventMs;
    private float visibility = 0f;
    private float progressAnimated;

    public MusicInfoWidget() {
        super(10f, 180f);
        TickEvent.getInstance().subscribe(new Listener<>(event -> onClientTick()));
    }

    @Override
    public String getName() {
        return "Music Info";
    }

    @Override
    public boolean shouldAppearWhenInterfaceVisible() {
        return mc.currentScreen instanceof ChatScreen
                || visibility > 0.01f
                || isMediaPlaying()
                || isMediaRecent();
    }

    private void onClientTick() {
        updateVisibilityTarget();
        if (!isEnabled() || !InterfaceModule.getInstance().isEnabled() || mc.player == null) {
            return;
        }

        if (mc.player.age % 5 != 0) {
            return;
        }

        mediaExecutor.execute(() -> {
            try {
                List<IMediaSession> sessions = MediaPlayerInfo.Instance.getMediaSessions();
                if (sessions == null || sessions.isEmpty()) {
                    return;
                }

                IMediaSession current = sessions.stream()
                        .filter(Objects::nonNull)
                        .filter(session -> session.getMedia() != null)
                        .max(Comparator.comparingInt(this::sessionScore))
                        .orElse(null);

                if (current == null) {
                    return;
                }

                MediaInfo info = current.getMedia();
                if (info == null) {
                    return;
                }

                if (info.getTitle().isEmpty() && info.getArtist().isEmpty() && !info.getPlaying()) {
                    return;
                }

                byte[] png = info.getArtworkPng();
                int artworkHash = png != null ? Arrays.hashCode(png) : 0;
                if (png != null && png.length > 0 && artworkHash != lastArtworkHash) {
                    lastArtworkHash = artworkHash;
                    mc.execute(() -> registerArtwork(png));
                }

                mediaInfo = info;
                currentOwner = current.getOwner();
                lastMediaEventMs = System.currentTimeMillis();
            } catch (Exception ignored) {
            }
        });
    }

    private int sessionScore(IMediaSession session) {
        try {
            MediaInfo info = session.getMedia();
            if (info == null) {
                return 0;
            }
            if (info.getPlaying()) {
                return 3;
            }
            if (!info.getTitle().isEmpty() || !info.getArtist().isEmpty()) {
                return 2;
            }
        } catch (Exception ignored) {
        }
        return 1;
    }

    private void updateVisibilityTarget() {
        boolean show = mc.currentScreen instanceof ChatScreen || isMediaRecent() || isMediaPlaying();
        visibility = MathUtil.interpolate(visibility, show ? 1f : 0f, 0.18f);
    }

    private boolean isMediaRecent() {
        return System.currentTimeMillis() - lastMediaEventMs < 5000L;
    }

    private boolean isMediaPlaying() {
        try {
            return mediaInfo != null && mediaInfo.getPlaying();
        } catch (Exception ignored) {
            return false;
        }
    }

    private void registerArtwork(byte[] png) {
        try {
            NativeImage image = NativeImage.read(new ByteArrayInputStream(png));
            NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
            mc.getTextureManager().registerTexture(artworkId, texture);
            artworkRegistered = true;
        } catch (Exception ignored) {
            artworkRegistered = false;
        }
    }

    @Override
    public void render(Render2DEvent.Render2DEventData event) {
        DrawContext context = event.context();
        MatrixStack matrixStack = context.getMatrices();
        updateVisibilityTarget();

        boolean inChat = mc.currentScreen instanceof ChatScreen;
        if (visibility < 0.02f && !inChat && !isMediaRecent() && !isMediaPlaying()) {
            return;
        }

        float vis = inChat ? 1f : visibility;
        if (vis < 0.02f) {
            return;
        }

        Layout layout = computeLayout();
        MediaInfo currentMedia = mediaInfo != null ? mediaInfo : EMPTY_MEDIA;
        float centerX = layout.x + layout.totalW * 0.5f;
        float centerY = layout.y + layout.totalH * 0.5f;

        matrixStack.push();
        matrixStack.translate(centerX, centerY, 0f);
        matrixStack.scale(vis, vis, 1f);
        matrixStack.translate(-centerX, -centerY, 0f);

        int alpha = vis == 1f ? 255 : (int)(vis * 255);
        drawHudCard(matrixStack, layout.x, layout.y, layout.totalW, layout.totalH, layout.borderR, alpha);

        drawArtwork(context, matrixStack, currentMedia, layout, alpha);
        drawTrackTexts(matrixStack, currentMedia, layout);
        drawControlPanel(matrixStack, currentMedia, layout);
        drawProgressPanel(matrixStack, currentMedia, layout);
        drawCloseButton(matrixStack, layout);

        matrixStack.pop();

        getDraggable().setWidth(layout.totalW);
        getDraggable().setHeight(layout.totalH);
    }

    private void drawArtwork(DrawContext context, MatrixStack matrixStack, MediaInfo currentMedia, Layout layout, int alpha) {
        if (artworkRegistered && currentMedia.getArtworkPng() != null && currentMedia.getArtworkPng().length > 0) {
            var texture = mc.getTextureManager().getTexture(artworkId);
            if (texture != null) {
                float coverRound = scaled(4f);
                RenderUtil.RECT.draw(matrixStack, layout.coverX, layout.coverY, layout.coverSize, layout.coverSize, coverRound, Color.WHITE);
                ScissorUtil.start(matrixStack, layout.coverX, layout.coverY, layout.coverSize, layout.coverSize);
                RenderUtil.TEXTURE_RECT.draw(matrixStack, layout.coverX, layout.coverY, layout.coverSize, layout.coverSize, coverRound,
                        Color.WHITE, 0f, 0f, 1f, 1f, texture.getGlId());
                ScissorUtil.stop(matrixStack);
                return;
            }
        }

        drawHudSquare(matrixStack, layout.coverX, layout.coverY, layout.coverSize, layout.coverSize, scaled(4f), alpha);
        Font font = getSemiBoldFont();
        String note = "♪";
        float size = scaled(16f);
        float textW = font.getWidth(note, size);
        float textH = font.getHeight(size);
        font.drawText(matrixStack, note,
                layout.coverX + layout.coverSize / 2f - textW / 2f,
                layout.coverY + layout.coverSize / 2f - textH / 2f,
                size,
                UIColors.primary());
    }

    private void drawTrackTexts(MatrixStack matrixStack, MediaInfo currentMedia, Layout layout) {
        Font textFont = getSemiBoldFont();
        float titleSize = scaled(7f);
        float artistSize = scaled(6.5f);
        String title = currentMedia.getTitle().isEmpty() ? "No media" : currentMedia.getTitle();
        String artist = currentMedia.getArtist().isEmpty() ? "Waiting for session" : currentMedia.getArtist();
        float textMax = layout.totalW - layout.pad * 2f;
        float textY = layout.coverY + layout.coverSize + layout.pad * 0.5f;

        drawClippedText(matrixStack, textFont, title, layout.x + layout.pad, textY, textMax, titleSize, UIColors.textColor());
        textY += titleSize + scaled(1f);
        drawClippedText(matrixStack, textFont, artist, layout.x + layout.pad, textY, textMax, artistSize, UIColors.inactiveTextColor());
    }

    private void drawControlPanel(MatrixStack matrixStack, MediaInfo currentMedia, Layout layout) {
        drawPanel(matrixStack, layout.panelX, layout.panelY, layout.panelW, layout.panelH, scaled(3f));

        Font buttonFont = getSemiBoldFont();
        float buttonSize = scaled(6.2f);
        drawButton(matrixStack, buttonFont, "<<", layout.prevX(), layout.btnY, layout.btnSize, buttonSize);
        drawButton(matrixStack, buttonFont, currentMedia.getPlaying() ? "||" : ">", layout.playX(), layout.btnY, layout.btnSize, buttonSize);
        drawButton(matrixStack, buttonFont, ">>", layout.nextX(), layout.btnY, layout.btnSize, buttonSize);
    }

    private void drawProgressPanel(MatrixStack matrixStack, MediaInfo currentMedia, Layout layout) {
        drawPanel(matrixStack, layout.progPanelX, layout.progPanelY, layout.progPanelW, layout.progPanelH, scaled(3f));

        long duration = currentMedia.getDuration();
        long position = currentMedia.getPosition();
        float targetProgress = duration > 0L ? Math.min(1f, (float) position / (float) duration) : 0f;
        progressAnimated = MathUtil.interpolate(progressAnimated, targetProgress, 0.22f);

        RenderUtil.RECT.draw(matrixStack, layout.progX, layout.progY, layout.progBarW, layout.progBarH, scaled(1.5f),
                new Color(40, 40, 50, 255));
        if (progressAnimated > 0.001f) {
            RenderUtil.RECT.draw(matrixStack, layout.progX, layout.progY,
                    layout.progBarW * Math.min(1f, progressAnimated), layout.progBarH,
                    scaled(1.5f), UIColors.primary());
        }
    }

    private void drawCloseButton(MatrixStack matrixStack, Layout layout) {
        Font font = getSemiBoldFont();
        String cross = "x";
        float closeSize = scaled(6f);
        float closeX = layout.x + layout.totalW - layout.pad - closeSize;
        float closeY = layout.y + layout.pad - scaled(0.5f);
        font.drawText(matrixStack, cross, closeX, closeY, closeSize, UIColors.inactiveTextColor());
    }

    private void drawPanel(MatrixStack matrixStack, float x, float y, float width, float height, float round) {
        drawHudSquare(matrixStack, x, y, width, height, round, 255);
    }

    private void drawButton(MatrixStack matrixStack, Font font, String label, float x, float y, float box, float size) {
        float textW = font.getWidth(label, size);
        float textH = font.getHeight(size);
        font.drawText(matrixStack, label,
                x + (box - textW) * 0.5f,
                y + (box - textH) * 0.5f,
                size,
                UIColors.primary());
    }

    private void drawClippedText(MatrixStack matrixStack, Font font, String text, float x, float y, float maxWidth, float size, Color color) {
        float width = font.getWidth(text, size);
        if (width <= maxWidth) {
            font.drawText(matrixStack, text, x, y, size, color);
            return;
        }

        ScissorUtil.start(matrixStack, x, y, maxWidth, size + scaled(2f));
        font.drawText(matrixStack, text, x, y, size, color);
        ScissorUtil.stop(matrixStack);
    }

    @Override
    public boolean handleMouseClick(double mouseX, double mouseY, int button) {
        if (button != 0 || !isEnabled() || !InterfaceModule.getInstance().isEnabled()) {
            return false;
        }

        boolean inChat = mc.currentScreen instanceof ChatScreen;
        boolean recent = isMediaRecent();
        boolean playing = isMediaPlaying();
        float vis = inChat ? 1f : visibility;
        if (vis < 0.02f && !inChat && !recent && !playing) {
            return false;
        }

        Layout layout = computeLayout();
        float mx = (float) mouseX;
        float my = (float) mouseY;
        if (!inChat) {
            float centerX = layout.x + layout.totalW * 0.5f;
            float centerY = layout.y + layout.totalH * 0.5f;
            mx = centerX + (mx - centerX) / Math.max(0.05f, vis);
            my = centerY + (my - centerY) / Math.max(0.05f, vis);
        }

        float closeSize = scaled(8f);
        float closeX = layout.x + layout.totalW - layout.pad - closeSize;
        float closeY = layout.y + layout.pad - scaled(1f);
        if (hit(mx, my, closeX - scaled(2f), closeY - scaled(1f), closeSize + scaled(4f), closeSize + scaled(2f))) {
            disableWidgetSetting();
            return true;
        }

        if (hit(mx, my, layout.prevX(), layout.btnY, layout.btnSize, layout.btnSize)) {
            invokeSession(IMediaSession::previous);
            return true;
        }
        if (hit(mx, my, layout.playX(), layout.btnY, layout.btnSize, layout.btnSize)) {
            invokeSession(IMediaSession::playPause);
            return true;
        }
        if (hit(mx, my, layout.nextX(), layout.btnY, layout.btnSize, layout.btnSize)) {
            invokeSession(IMediaSession::next);
            return true;
        }

        return false;
    }

    private void disableWidgetSetting() {
        setEnabled(false);
        InterfaceModule.getInstance().widgets.getValue().stream()
                .filter(setting -> setting.getName().equals(getName()))
                .findFirst()
                .ifPresent(setting -> setting.value(false));
    }

    private boolean hit(float mouseX, float mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private void invokeSession(Consumer<IMediaSession> action) {
        String owner = currentOwner;
        mediaExecutor.execute(() -> {
            try {
                List<IMediaSession> sessions = MediaPlayerInfo.Instance.getMediaSessions();
                if (sessions == null || sessions.isEmpty()) {
                    return;
                }

                IMediaSession session = sessions.stream()
                        .filter(Objects::nonNull)
                        .filter(candidate -> owner.isEmpty() || owner.equals(candidate.getOwner()))
                        .findFirst()
                        .orElse(sessions.getFirst());

                action.accept(session);
                lastMediaEventMs = System.currentTimeMillis();
            } catch (Exception ignored) {
            }
        });
    }

    @Override
    public void render(MatrixStack matrixStack) {
    }

    private Layout computeLayout() {
        float x = getDraggable().getX();
        float y = getDraggable().getY();
        float pad = scaled(5f);
        float totalW = scaled(90f);
        float coverSize = totalW - pad * 2f;
        float borderR = scaled(4f);

        float titleBlock = scaled(7f) + scaled(1f) + scaled(6.5f) + scaled(2f);
        float panelH = scaled(22f);
        float progPanelH = scaled(10f);
        float gap = scaled(4f);
        float totalH = pad + coverSize + titleBlock + gap + panelH + pad + progPanelH + pad;

        float coverX = x + pad;
        float coverY = y + pad;

        float panelY = coverY + coverSize + titleBlock + gap;
        float panelW = totalW - pad * 2f;
        float panelX = x + pad;

        float btnSize = scaled(16f);
        float btnGap = scaled(8f);
        float rowW = btnSize * 3f + btnGap * 2f;
        float buttonsStartX = x + (totalW - rowW) * 0.5f;
        float btnY = panelY + (panelH - btnSize) * 0.5f;

        float progPanelY = panelY + panelH + pad;
        float progPanelW = panelW;
        float progPanelX = panelX;
        float progPad = scaled(6f);
        float progBarH = scaled(3f);
        float progY = progPanelY + (progPanelH - progBarH) * 0.5f;
        float progBarW = progPanelW - progPad * 2f;
        float progX = progPanelX + progPad;

        return new Layout(x, y, totalW, totalH, pad, borderR, coverX, coverY, coverSize,
                panelX, panelY, panelW, panelH, buttonsStartX, btnY, btnSize, btnGap,
                progPanelX, progPanelY, progPanelW, progPanelH, progX, progY, progBarW, progBarH);
    }

    private record Layout(float x, float y, float totalW, float totalH, float pad, float borderR,
                          float coverX, float coverY, float coverSize,
                          float panelX, float panelY, float panelW, float panelH,
                          float buttonsStartX, float btnY, float btnSize, float btnGap,
                          float progPanelX, float progPanelY, float progPanelW, float progPanelH,
                          float progX, float progY, float progBarW, float progBarH) {
        float prevX() {
            return buttonsStartX;
        }

        float playX() {
            return buttonsStartX + btnSize + btnGap;
        }

        float nextX() {
            return playX() + btnSize + btnGap;
        }
    }
}
