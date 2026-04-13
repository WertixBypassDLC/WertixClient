package sweetie.nezi.api.utils.render.fonts;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;
import sweetie.nezi.api.system.backend.ClientInfo;
import sweetie.nezi.api.system.files.FileUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class FontBuilder {
    private String name;
    private Identifier dataIdentifier;
    private Identifier atlasIdentifier;

    public FontBuilder() {}

    public FontBuilder find(String fontName) {
        this.name = fontName;

        // Очищаем имя от возможных лишних слэшей и переводим в нижний регистр
        String cleanName = fontName.replace("/", "").toLowerCase();
        String namespace = ClientInfo.NAME.toLowerCase();

        // Формируем идентификаторы без риска получить двойной слэш
        this.dataIdentifier = Identifier.of(namespace, "fonts/" + cleanName + ".json");
        this.atlasIdentifier = Identifier.of(namespace, "fonts/" + cleanName + ".png");

        return this;
    }

    public Font load() {
        FontData data = FileUtil.fromJsonToInstance(this.dataIdentifier, FontData.class);

        if (data == null) {
            // Более информативная ошибка для отладки
            throw new RuntimeException("Failed to read font data: " + this.dataIdentifier.toString());
        }

        AbstractTexture texture = MinecraftClient.getInstance().getTextureManager().getTexture(this.atlasIdentifier);

        RenderSystem.recordRenderCall(() -> {
            if (texture != null) {
                texture.setFilter(true, false);
            }
        });

        float aWidth = data.atlas().width();
        float aHeight = data.atlas().height();

        Map<Integer, MsdfGlyph> glyphs = data.glyphs().stream()
                .collect(Collectors.toMap(
                        FontData.GlyphData::unicode,
                        (glyphData) -> new MsdfGlyph(glyphData, aWidth, aHeight),
                        (existing, replacement) -> existing // Защита от дубликатов юникода
                ));

        Map<Integer, Map<Integer, Float>> kernings = new HashMap<>();
        data.kernings().forEach((kerning) -> {
            kernings.computeIfAbsent(kerning.leftChar(), k -> new HashMap<>())
                    .put(kerning.rightChar(), kerning.advance());
        });

        return new Font(name, texture, data.atlas(), data.metrics(), glyphs, kernings);
    }
}