package sweetie.nezi.api.module;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Category {
    COMBAT("Combat", "Бой"),
    MOVEMENT("Movement", "Движение"),
    RENDER("Render", "Визуал"),
    PLAYER("Player", "Игрок"),
    OTHER("Other", "Другое");

    private final String englishLabel;
    private final String russianLabel;
}
