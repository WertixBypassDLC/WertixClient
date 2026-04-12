package sweetie.nezi.api.utils.other;

import lombok.experimental.UtilityClass;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import sweetie.nezi.api.system.backend.ClientInfo;
import sweetie.nezi.api.system.interfaces.QuickImports;
import sweetie.nezi.client.features.modules.other.ToggleSoundsModule;

@UtilityClass
public class SoundUtil implements QuickImports {

    // celestial toggle sound
    private final Identifier ENABLE_CEL_SOUND = Identifier.of(path() + "celestial_on");
    public final SoundEvent ENABLE_CEL_EVENT = SoundEvent.of(ENABLE_CEL_SOUND);

    private final Identifier DISABLE_CEL_SOUND = Identifier.of(path() + "celestial_off");
    public final SoundEvent DISABLE_CEL_EVENT = SoundEvent.of(DISABLE_CEL_SOUND);

    // blop toggle sound
    private final Identifier ENABLE_BLOP_SOUND = Identifier.of(path() + "blop_on");
    public final SoundEvent ENABLE_BLOP_EVENT = SoundEvent.of(ENABLE_BLOP_SOUND);

    private final Identifier DISABLE_BLOP_SOUND = Identifier.of(path() + "blop_off");
    public final SoundEvent DISABLE_BLOP_EVENT = SoundEvent.of(DISABLE_BLOP_SOUND);

    public void load() {
        Registry.register(Registries.SOUND_EVENT, ENABLE_CEL_SOUND, ENABLE_CEL_EVENT);
        Registry.register(Registries.SOUND_EVENT, DISABLE_CEL_SOUND, DISABLE_CEL_EVENT);

        Registry.register(Registries.SOUND_EVENT, ENABLE_BLOP_SOUND, ENABLE_BLOP_EVENT);
        Registry.register(Registries.SOUND_EVENT, DISABLE_BLOP_SOUND, DISABLE_BLOP_EVENT);
    }

    public void playSound(SoundEvent sound) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.world == null || client.getCameraEntity() == null) return;

        client.world.playSound(
                client.player,
                client.getCameraEntity().getBlockPos(),
                sound,
                SoundCategory.BLOCKS,
                ToggleSoundsModule.getInstance().volume.getValue() / 100f,
                1f
        );
    }

    private String path() {
        return ClientInfo.NAME.toLowerCase() + ":";
    }
}
