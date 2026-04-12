package sweetie.nezi.api.system.backend;

import lombok.experimental.UtilityClass;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.nio.file.Path;

@UtilityClass
public class ClientInfo {
    public final String MOD_ID = "cullleaves";
    public final String NAME = "nezi";
    public final String VERSION = FabricLoader.getInstance()
            .getModContainer(MOD_ID)
            .orElseThrow()
            .getMetadata()
            .getVersion()
            .getFriendlyString();

    public final String GAME_PATH = new File(System.getProperty("user.dir")).getAbsolutePath();
    public final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir();
    public final String CONFIG_PATH = CONFIG_DIR.resolve(MOD_ID + ".json").toAbsolutePath().toString();

    // Kept for backward compatibility with older code paths.
    public final String CONFIG_PATH_THEMES = CONFIG_PATH;
    public final String CONFIG_PATH_OTHER = CONFIG_PATH;
}
