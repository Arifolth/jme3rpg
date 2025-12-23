package ru.arifolth.anjrpg.worldmap;

import com.jme3.texture.Texture;
import ru.arifolth.anjrpg.interfaces.worldmap.ITextureCache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;

public class TextureCache implements ITextureCache {
    private static ITextureCache instance;
    private final ConcurrentHashMap<String, Texture> cache;

    private TextureCache() {
        this.cache = new ConcurrentHashMap<>();
    }

    public static synchronized ITextureCache getInstance() {
        if (instance == null) {
            instance = new TextureCache();
        }
        return instance;
    }

    public void storeTexture(String tileId, Texture texture) {
        if (tileId != null && texture != null) {
            cache.put(tileId, texture);
        }
    }

    public Optional<Texture> getTexture(String tileId) {
        return Optional.ofNullable(cache.get(tileId));
    }

    public boolean containsTexture(String tileId) {
        return cache.containsKey(tileId);
    }

    public void clear() {
        cache.clear();
    }

    public int size() {
        return cache.size();
    }
}
