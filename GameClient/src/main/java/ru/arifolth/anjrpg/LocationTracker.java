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
import ru.arifolth.anjrpg.compass.CompassState;
import ru.arifolth.anjrpg.compass.POITarget;
import ru.arifolth.anjrpg.events.LocationFoundEvent;
import ru.arifolth.anjrpg.interfaces.CharacterInterface;
import ru.arifolth.anjrpg.interfaces.Constants;
import ru.arifolth.anjrpg.interfaces.GameLogicCoreInterface;
import ru.arifolth.anjrpg.interfaces.TrackerInterface;
import ru.arifolth.anjrpg.interfaces.compass.POIInterface;
import ru.arifolth.anjrpg.interfaces.compass.POIType;
import ru.arifolth.anjrpg.quest.Quest;
import ru.arifolth.anjrpg.quest.StorySegment;
import ru.arifolth.anjrpg.quest.task.FindTask;
import ru.arifolth.anjrpg.quest.task.KillTask;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LocationTracker implements TrackerInterface {
    final private static Logger LOGGER = Logger.getLogger(LocationTracker.class.getName());

    private GameLogicCoreInterface gameLogicCore;
    // POIs the player is currently standing in; a LocationFoundEvent is published only on entering, not every frame
    private final Set<String> reportedPOIs = new HashSet<>();
    private boolean firstRun = true;

    public LocationTracker(GameLogicCoreInterface gameLogicCore) {
        this.gameLogicCore = gameLogicCore;
    }

    @Override
    public void update(float tpf) {
        checkNPCs();

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

    private void checkNPCs() {
        if(firstRun) {
            startTestQuest();
            firstRun = false;
        }
    }

    /**
     * Throwaway, hardcoded quest so the whole loop (events -> tasks -> journal)
     * can be exercised before any quest-giver system exists. Segment 1: kill up
     * to three of the NPCs standing around right now; segment 2: walk to a
     * waystone a short way from the start. Remove once quests are started by
     * NPC dialogue.
     */
    private void startTestQuest() {
        Set<CharacterInterface> targets = new HashSet<>();
        for (CharacterInterface npc : gameLogicCore.getCharacterMap().values()) {
            if (targets.size() < 3) {
                targets.add(npc);
            }
        }

        if (targets.isEmpty()) {
            LOGGER.log(Level.INFO, "No NPCs present - test quest not started");
            return;
        }

        POITarget waystone = new POITarget("test_waystone",
                gameLogicCore.getPlayerCharacter().getCharacterControl().getPhysicsLocation().add(300f, 0f, 0),
                "Old Waystone", POIType.LANDMARK);
        LOGGER.log(Level.INFO, "Test quest waystone at " + waystone.getPosition());

        CompassState compassState = gameLogicCore.getApp().getStateManager().getState(CompassState.class);
        compassState.addPOI(waystone);

        Quest quest = new Quest("test_quest", "Test Quest", List.of(
                new StorySegment("Clear the way", "Hostile creatures roam nearby. Defeat them.")
                        .addRequiredTask(new KillTask("test_kill", "defeat " + targets.size() + " hostile creatures", targets)),
                new StorySegment("The waystone", "Find the Old Waystone.")
                        .addRequiredTask(new FindTask("test_find", "find the Old Waystone", waystone))
        ));

        gameLogicCore.getQuestService().startQuest(quest);
    }

}
