package sweetie.nezi.api.system.draggable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.other.WindowResizeEvent;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.system.interfaces.QuickImports;
import sweetie.nezi.api.utils.math.MathUtil;

@Getter
@Setter
public class Draggable implements QuickImports {
    @Expose
    @SerializedName("x")
    private float x;
    @Expose
    @SerializedName("y")
    private float y;

    public float initialXVal;
    public float initialYVal;

    private float startX, startY;
    private boolean dragging;
    private float width = 0f;
    private float height = 0f;
    private boolean wasDragging;
    private float releasePulse;
    @Expose
    @SerializedName("name")
    private final String name;
    private final Module module;

    /** Last-known screen dimensions — used to remap position on resize. */
    private float lastScreenWidth;
    private float lastScreenHeight;

    public Draggable(Module module, String name, float initialXVal, float initialYVal) {
        this.module = module;
        this.name = name;
        this.x = roundToHalf(initialXVal);
        this.y = roundToHalf(initialYVal);
        this.initialXVal = initialXVal;
        this.initialYVal = initialYVal;

        // Initialise last-known screen size
        if (mc != null && mc.getWindow() != null) {
            lastScreenWidth = mc.getWindow().getScaledWidth();
            lastScreenHeight = mc.getWindow().getScaledHeight();
        }

        WindowResizeEvent.getInstance().subscribe(new Listener<>(-1, event -> {
            float newW = mc.getWindow().getScaledWidth();
            float newH = mc.getWindow().getScaledHeight();

            if (lastScreenWidth > 0 && lastScreenHeight > 0 && newW > 0 && newH > 0
                    && (Math.abs(lastScreenWidth - newW) > 0.5f || Math.abs(lastScreenHeight - newH) > 0.5f)) {
                // Proportional remapping: keep the same relative screen position
                float ratioX = newW / lastScreenWidth;
                float ratioY = newH / lastScreenHeight;
                x = roundToHalf(x * ratioX);
                y = roundToHalf(y * ratioY);
                clampToScreen();
            } else if (dragging) {
                clampToScreen();
            }

            lastScreenWidth = newW;
            lastScreenHeight = newH;
        }));
    }

    public final void onDraw() {
        if (!dragging && wasDragging) {
            releasePulse = 1.0f;
        }
        wasDragging = dragging;

        if (dragging) {
            x = roundToHalf(MathUtil.interpolate(x, (normaliseX() - startX), .15f));
            y = roundToHalf(MathUtil.interpolate(y, (normaliseY() - startY), .15f));

            softClampToScreen();
        }

        releasePulse = Math.max(0f, releasePulse + (0f - releasePulse) * 0.12f);
        if (releasePulse < 0.01f) {
            releasePulse = 0f;
        }
    }

    public final void onClick(int button) {
        if (button == 0 && isHovering()) {
            boolean anotherDragging = DraggableManager.getInstance().getDraggables().values().stream().anyMatch(Draggable::isDragging);
            if (!anotherDragging) {
                dragging = true;
                startX = (int) (normaliseX() - x);
                startY = (int) (normaliseY() - y);
            }
        }
    }

    public final void onRelease(int button) {
        if (button == 0) {
            dragging = false;
            clampToScreen();
        }
    }

    /**
     * Used by HUD widgets to apply effects (tilt) only while the user is actually dragging.
     */
    public boolean isDragging() {
        return dragging;
    }

    public boolean isHovering() {
        return normaliseX() > Math.min(x, x + width) && normaliseX() < Math.max(x, x + width) && normaliseY() > Math.min(y, y + height) && normaliseY() < Math.max(y, y + height);
    }

    public int normaliseX() {
        return (int) (mc.mouse.getX() / mc.getWindow().getScaleFactor());
    }

    public int normaliseY() {
        return (int) (mc.mouse.getY() / mc.getWindow().getScaleFactor());
    }

    private float roundToHalf(float value) {
        return Math.round(value * 2) / 2.0f;
    }

    private void clampToScreen() {
        clampToScreen(3f, 0f);
    }

    private void softClampToScreen() {
        float overflow = Math.max(10f, Math.max(width, height) * 0.2f);
        clampToScreen(3f, overflow);
    }

    private void clampToScreen(float margin, float overflow) {
        float screenWidth = mc.getWindow().getScaledWidth();
        float screenHeight = mc.getWindow().getScaledHeight();

        float minX = margin - overflow;
        float minY = margin - overflow;
        float maxX = screenWidth - width - margin + overflow;
        float maxY = screenHeight - height - margin + overflow;

        if (x < minX) x = minX;
        if (y < minY) y = minY;
        if (x > maxX) x = maxX;
        if (y > maxY) y = maxY;
    }
}
