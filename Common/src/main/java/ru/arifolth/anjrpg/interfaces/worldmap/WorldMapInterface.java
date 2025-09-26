/**
 * ANJRpg - an open source Role Playing Game written in Java.
 * Copyright (C) 2014 - 2025 Alexander Nilov
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package ru.arifolth.anjrpg.interfaces.worldmap;

import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import ru.arifolth.anjrpg.interfaces.CharacterInterface;
import ru.arifolth.anjrpg.interfaces.compass.POIInterface;

import java.util.Collection;
import java.util.Set;


public interface WorldMapInterface {
    void updateFogOfWar(Set<String> discoveredTiles);

    void updatePOIMarkers(Collection<POIInterface> pois, CharacterInterface playerCharacter,
                          Vector3f mapCenter, float mapScale);

    void updateTileGrid(Set<String> discoveredTiles, int gridCenterX, int gridCenterZ);

    Node getMapNode();

    boolean isVisible();

    void setVisible(boolean visible);
}
