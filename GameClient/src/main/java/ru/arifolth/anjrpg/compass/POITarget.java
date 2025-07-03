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

package ru.arifolth.anjrpg.compass;

import com.jme3.math.Vector3f;
import ru.arifolth.anjrpg.interfaces.compass.POIInterface;
import ru.arifolth.anjrpg.interfaces.compass.POIType;

/**
 * Represents a Point Of Interest (POI) target for the compass system.
 * Can be a fixed landmark or a moving NPC.
 */
public class POITarget implements POIInterface {
    private final String id;           // Unique identifier for the POI
    private Vector3f position;         // World coordinates of the POI
    private String name;               // Optional display name
    private POIType type;              // Type of POI (LANDMARK, NPC, etc.)

    /**
     * Constructor for a POI target.
     * @param id Unique ID string
     * @param position World position vector
     * @param name Optional name for display
     * @param type Type of the POI
     */
    public POITarget(String id, Vector3f position, String name, POIType type) {
        this.id = id;
        this.position = position.clone();
        this.name = name;
        this.type = type;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Vector3f getPosition() {
        return position.clone();
    }

    @Override
    public void setPosition(Vector3f newPosition) {
        this.position = newPosition.clone();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String newName) {
        this.name = newName;
    }

    @Override
    public POIType getType() {
        return type;
    }

    @Override
    public void setType(POIType newType) {
        this.type = newType;
    }
}