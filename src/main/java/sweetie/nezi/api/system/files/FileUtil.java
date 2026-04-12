package sweetie.nezi.api.system.files;

import com.google.gson.Gson;
import lombok.experimental.UtilityClass;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import sweetie.nezi.api.system.backend.ClientInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@UtilityClass
public class FileUtil {
    private final Gson GSON = new Gson();

    public InputStream getFromAssets(String input) {
        return FileUtil.class.getResourceAsStream("/assets/" + ClientInfo.NAME.toLowerCase() + "/" + normalizePath(input));
    }

    public Identifier getImage(String path) {
        return Identifier.of(ClientInfo.NAME.toLowerCase(), "images/" + path + ".png");
    }

    public Identifier getShader(String name) {
        return Identifier.of(ClientInfo.NAME.toLowerCase(), "core/" + name);
    }

    public <T> T fromJsonToInstance(Identifier identifier, Class<T> clazz) {
        try {
            return GSON.fromJson(toString(identifier), clazz);
        } catch (Exception e) {
            System.err.println("Failed to deserialize JSON from " + identifier + " to " + clazz.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public String toString(Identifier identifier) {
        return toString(identifier, "\n");
    }

    public String toString(Identifier identifier, String delimiter) {
        try(InputStream inputStream = openResource(identifier);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining(delimiter));
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read resource " + identifier, ex);
        }
    }

    private InputStream openResource(Identifier identifier) throws IOException {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.getResourceManager() != null) {
            try {
                return client.getResourceManager().open(identifier);
            } catch (IOException ignored) {
                // Fall through to classpath lookup during early startup or missing pack resources.
            }
        }

        InputStream fallbackStream = FileUtil.class.getResourceAsStream(toClasspathPath(identifier));
        if (fallbackStream != null) {
            return fallbackStream;
        }

        throw new IOException("Resource not found: " + identifier);
    }

    private String toClasspathPath(Identifier identifier) {
        String namespace = identifier.getNamespace() == null || identifier.getNamespace().isBlank()
                ? ClientInfo.NAME.toLowerCase()
                : identifier.getNamespace();
        return "/assets/" + namespace + "/" + normalizePath(identifier.getPath());
    }

    private String normalizePath(String path) {
        String normalized = path == null ? "" : path.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.contains("//")) {
            normalized = normalized.replace("//", "/");
        }
        return normalized;
    }
}
