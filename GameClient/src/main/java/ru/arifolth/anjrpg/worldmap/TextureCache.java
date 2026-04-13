/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2014 - 2025 Alexander Nilov
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
            cache.putIfAbsent(tileId, texture);
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
