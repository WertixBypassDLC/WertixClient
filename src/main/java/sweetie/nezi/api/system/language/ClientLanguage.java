package sweetie.nezi.api.system.language;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ClientLanguage {
    ENGLISH("English"),
    RUSSIAN("Russian");

    private final String modeName;

    public static ClientLanguage fromModeName(String value) {
        for (ClientLanguage language : values()) {
            if (language.modeName.equalsIgnoreCase(value)) {
                return language;
            }
        }
        return RUSSIAN;
    }
}
