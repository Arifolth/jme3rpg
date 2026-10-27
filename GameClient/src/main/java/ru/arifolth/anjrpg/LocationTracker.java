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

package ru.arifolth.anjrpg;

import com.jme3.math.Vector3f;
import ru.arifolth.anjrpg.events.LocationFoundEvent;
import ru.arifolth.anjrpg.interfaces.CharacterInterface;
import ru.arifolth.anjrpg.interfaces.Constants;
import ru.arifolth.anjrpg.interfaces.GameLogicCoreInterface;
import ru.arifolth.anjrpg.interfaces.TrackerInterface;
import ru.arifolth.anjrpg.interfaces.compass.POIInterface;

import java.util.HashSet;
import java.util.Set;

public class LocationTracker implements TrackerInterface {
    private GameLogicCoreInterface gameLogicCore;
    // POIs the player is currently standing in; a LocationFoundEvent is published only on entering, not every frame
    private final Set<String> reportedPOIs = new HashSet<>();

    public LocationTracker(GameLogicCoreInterface gameLogicCore) {
        this.gameLogicCore = gameLogicCore;
    }

    @Override
    public void update(float tpf) {
        checkQuestLocations();
    }

    /**
     * Publishes a {@link LocationFoundEvent} when the player walks into the
     * radius of a POI that an active quest task is looking for. Edge-triggered:
     * once per entry, so the event is not repeated every frame while the player
     * stands there, and fires again if the player leaves and comes back.
     * Horizontal (XZ) distance only - POI positions are authored without
     * regard to terrain height.
     */
    private void checkQuestLocations() {
        CharacterInterface player = gameLogicCore.getPlayerCharacter();
        if(player == null || player.isDead()) {
            return;
        }

        Vector3f playerPos = player.getCharacterControl().getPhysicsLocation();
        Set<String> inRange = new HashSet<>();

        for(POIInterface poi : gameLogicCore.getQuestService().getActiveLocationTargets()) {
            Vector3f poiPos = poi.getPosition();
            float dx = poiPos.x - playerPos.x;
            float dz = poiPos.z - playerPos.z;

            if(dx * dx + dz * dz <= Constants.LOCATION_FOUND_RANGE * Constants.LOCATION_FOUND_RANGE) {
                inRange.add(poi.getId());
                if(reportedPOIs.add(poi.getId())) {
                    gameLogicCore.getEventBus().publish(new LocationFoundEvent(poi.getId(), player));
                }
            }
        }

        reportedPOIs.retainAll(inRange);
    }
}
