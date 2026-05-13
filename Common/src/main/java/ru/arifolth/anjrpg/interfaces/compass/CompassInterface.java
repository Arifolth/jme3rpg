/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2014 - 2026 Alexander Nilov
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
package ru.arifolth.anjrpg.interfaces.compass;

import com.jme3.scene.Geometry;
import com.jme3.scene.Node;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface CompassInterface {
    Collection<POIInterface> getPointsOfInterest();

    void setPointsOfInterest(Collection<POIInterface> pointsOfInterest);

    void updateCompassRotation(float compassOffset);

    Geometry getTargetIndicator(String poiId);

    float getCompassWidth();

    Node getCompassNode();

    Map<String, Geometry> getTargetIndicators();
}
