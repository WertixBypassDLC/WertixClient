package sweetie.nezi.client.ui.widget;

import lombok.Getter;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Quaternionf;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.render.Render2DEvent;
import sweetie.nezi.client.features.modules.render.InterfaceModule;
import sweetie.nezi.client.ui.widget.overlay.*;

import java.util.ArrayList;
import java.util.List;

@Getter
public class WidgetManager {
    @Getter private final static WidgetManager instance = new WidgetManager();

    private final List<Widget> widgets = new ArrayList<>();

    public void load() {
        register(
                new WatermarkWidget(),
                new ArrayListWidget(),
                new KeybindsWidget(),
                new PotionsWidget(),
                new StaffsWidget(),
                new CooldownsWidget(),
                new ArmorWidget(),
                new TargetInfoWidget(),
                new MusicInfoWidget(),
                new NotifWidget()
        );

        InterfaceModule.getInstance().init();

        Render2DEvent.getInstance().subscribe(new Listener<>(event -> {
            MatrixStack ms = event.matrixStack();
            boolean interfaceVisible = InterfaceModule.getInstance().isEnabled();

            for (Widget widget : widgets) {
                if (!widget.isEnabled()) continue;

                boolean shouldShow = interfaceVisible && widget.shouldAppearWhenInterfaceVisible();
                widget.updateAppear(shouldShow);

                float appear = widget.getAppearProgress();
                if (appear <= 0.01f && !widget.isTargetVisible() && !widget.getDraggable().isDragging()) continue;

                widget.updateTilt();

                float cx = widget.getDraggable().getX() + widget.getDraggable().getWidth() / 2f;
                float cy = widget.getDraggable().getY() + widget.getDraggable().getHeight() / 2f;
                ms.push();
                ms.translate(cx, cy, 0);
                float tilt = widget.getTiltAngle();
                float jitterX = widget.getDragJitterX();
                float jitterY = widget.getDragJitterY();
                if (Math.abs(jitterX) > 0.01f || Math.abs(jitterY) > 0.01f) {
                    ms.translate(jitterX, jitterY, 0);
                }
                if (Math.abs(tilt) > 0.01f)
                    ms.multiply(new Quaternionf().rotateZ((float) Math.toRadians(tilt)));
                ms.translate(-cx, -cy, 0);
                widget.renderReleasePulse(ms);
                widget.renderTransitionParticles(ms, true);
                widget.render(event);
                widget.renderTransitionParticles(ms, false);
                ms.pop();
            }
        }));
    }

    public void register(Widget... widgets) {
        this.widgets.addAll(List.of(widgets));
    }

    public boolean handleMouseClick(double mouseX, double mouseY, int button) {
        if (!InterfaceModule.getInstance().isEnabled()) {
            return false;
        }

        for (int i = widgets.size() - 1; i >= 0; i--) {
            Widget widget = widgets.get(i);
            if (!widget.isEnabled()) {
                continue;
            }

            if (widget.handleMouseClick(mouseX, mouseY, button)) {
                return true;
            }
        }

        return false;
    }
}
