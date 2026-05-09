package sweetie.nezi.client.ui.widget.overlay;

import net.minecraft.client.util.math.MatrixStack;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleManager;
import sweetie.nezi.api.utils.render.fonts.Fonts;
import sweetie.nezi.client.ui.widget.Widget;

import java.util.*;

public class ArrayListWidget extends Widget {

    private final Map<Module, Float> animMap  = new HashMap<>();
    private final Map<Module, Float> yAnimMap = new HashMap<>();
    private final Map<Module, Float> xAnimMap = new HashMap<>();

    public ArrayListWidget() { super(4f, 4f); }
    @Override public String getName() { return "ArrayList"; }

    @Override
    public void render(MatrixStack ms) {
        if (mc.player == null) return;

        float fS = scaled(6.1f);

        // --- Fade-in/out animations ---
        for (Module m : ModuleManager.getInstance().getModules()) {
            float cur    = animMap.getOrDefault(m, 0f);
            float target = m.isEnabled() ? 1f : 0f;
            animMap.put(m, cur + (target - cur) * 0.15f);
        }
        animMap.entrySet().removeIf(e -> !e.getKey().isEnabled() && e.getValue() < 0.01f);

        List<Module> visible = new ArrayList<>();
        for (Map.Entry<Module, Float> e : animMap.entrySet())
            if (e.getValue() > 0.01f) visible.add(e.getKey());

        visible.sort((a, b) -> Float.compare(
                Fonts.PS_MEDIUM.getWidth(b.getLocalizedName(), fS),
                Fonts.PS_MEDIUM.getWidth(a.getLocalizedName(), fS)));

        if (visible.isEmpty()) return;

        // getDraggable().getX() is always the LEFT edge of the draggable box.
        // We NEVER change it here — only setWidth/setHeight.
        float dragX = getDraggable().getX();
        float dragY = getDraggable().getY();
        float sw    = mc.getWindow() != null ? mc.getWindow().getScaledWidth() : 0f;

        float rowH  = scaled(10.8f);
        float gap   = scaled(1.5f);
        float p     = scaled(5f);
        float round = scaled(2.5f);

        // Compute maxW first
        float maxW = 0f;
        for (Module m : visible) {
            float tw = Fonts.PS_MEDIUM.getWidth(m.getLocalizedName(), fS);
            maxW = Math.max(maxW, tw + p * 2f);
        }

        // Side detection: if centre of the widget is past screen midpoint → right side
        float widgetCenterX = dragX + maxW / 2f;
        boolean rightSide = widgetCenterX > sw / 2f;

        // Build target Y positions
        float cY = dragY;
        Map<Module, Float> targetYMap = new LinkedHashMap<>();
        for (Module m : visible) {
            targetYMap.put(m, cY);
            cY += (rowH + gap) * animMap.getOrDefault(m, 0f);
        }

        // Animate Y and X positions
        for (Module m : visible) {
            float tgtY = targetYMap.get(m);
            float curY = yAnimMap.getOrDefault(m, tgtY);
            curY += (tgtY - curY) * 0.15f;
            if (Math.abs(curY - tgtY) < 0.5f) curY = tgtY;
            yAnimMap.put(m, curY);

            float tw = Fonts.PS_MEDIUM.getWidth(m.getLocalizedName(), fS);
            float w  = tw + p * 2f;
            // On left  side: entry starts at dragX (left edge).
            // On right side: entry ends at dragX + maxW (right edge of hitbox),
            //   so it starts at (dragX + maxW - w).
            float tgtX = rightSide ? (dragX + maxW - w) : dragX;
            float curX = xAnimMap.getOrDefault(m, tgtX);
            curX += (tgtX - curX) * 0.15f;
            if (Math.abs(curX - tgtX) < 0.5f) curX = tgtX;
            xAnimMap.put(m, curX);
        }
        yAnimMap.keySet().retainAll(animMap.keySet());
        xAnimMap.keySet().retainAll(animMap.keySet());

        // Draw
        for (Module m : visible) {
            float anim  = animMap.getOrDefault(m, 0f);
            float drawY = yAnimMap.getOrDefault(m, dragY);
            float tw    = Fonts.PS_MEDIUM.getWidth(m.getLocalizedName(), fS);
            float w     = tw + p * 2f;
            float drawX = xAnimMap.getOrDefault(m, rightSide ? (dragX + maxW - w) : dragX);
            int   al    = Math.max(5, (int)(255f * anim));

            // Scale pivot: expand from right edge on right side, left edge on left side
            float pivotX = rightSide ? (dragX + maxW) : dragX;

            ms.push();
            ms.translate(pivotX, drawY + rowH / 2f, 0f);
            ms.scale(anim, 1f, 1f);
            ms.translate(-pivotX, -(drawY + rowH / 2f), 0f);

            drawHudCard(ms, drawX, drawY, w, rowH, round, al);

            Fonts.PS_MEDIUM.drawText(ms, m.getLocalizedName(),
                    drawX + p,
                    drawY + rowH / 2f - fS / 2f + scaled(0.3f),
                    fS, widgetTextColor(al));

            ms.pop();
        }

        // Only update width/height — never move the X anchor (that causes the feedback loop bug)
        getDraggable().setWidth(maxW);
        getDraggable().setHeight(cY - dragY);
    }
}
