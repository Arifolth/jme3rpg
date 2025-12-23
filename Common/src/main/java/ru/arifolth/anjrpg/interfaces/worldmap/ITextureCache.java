package ru.arifolth.anjrpg.interfaces.worldmap;

import com.jme3.texture.Texture;
import java.util.Optional;

public interface ITextureCache {
    void storeTexture(String tileId, Texture texture);
    Optional<Texture> getTexture(String tileId);
    boolean containsTexture(String tileId);
    void clear();
    int size();
}
