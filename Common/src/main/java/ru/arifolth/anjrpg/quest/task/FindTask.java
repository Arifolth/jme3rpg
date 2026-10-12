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

package ru.arifolth.anjrpg.quest.task;

import ru.arifolth.anjrpg.events.EventBus;
import ru.arifolth.anjrpg.events.GameEventListener;
import ru.arifolth.anjrpg.events.ItemFoundEvent;
import ru.arifolth.anjrpg.events.LocationFoundEvent;
import ru.arifolth.anjrpg.events.NpcTalkedToEvent;
import ru.arifolth.anjrpg.interfaces.CharacterInterface;
import ru.arifolth.anjrpg.interfaces.compass.POIInterface;
import ru.arifolth.anjrpg.quest.Quest;

/**
 * Find a location, an item, or an NPC - which of the three is decided by the
 * constructor used, and decides which event the task listens to:
 * <ul>
 *     <li>{@code POIInterface} - completes on {@link LocationFoundEvent} for that POI's id</li>
 *     <li>{@code String itemId} - completes on {@link ItemFoundEvent} for that item</li>
 *     <li>{@code CharacterInterface} - completes on {@link NpcTalkedToEvent} for that NPC
 *     (finding an NPC means talking to it, not just walking past)</li>
 * </ul>
 */
public class FindTask extends QuestTask {
    private final POIInterface targetPOI;       // location mode
    private final String itemId;                // item mode
    private final CharacterInterface targetNpc; // npc mode

    private GameEventListener<LocationFoundEvent> locationListener;
    private GameEventListener<ItemFoundEvent> itemListener;
    private GameEventListener<NpcTalkedToEvent> npcListener;

    public FindTask(String taskId, String description, POIInterface targetPOI) {
        super(taskId, description);
        this.targetPOI = targetPOI;
        this.itemId = null;
        this.targetNpc = null;
    }

    public FindTask(String taskId, String description, String itemId) {
        super(taskId, description);
        this.targetPOI = null;
        this.itemId = itemId;
        this.targetNpc = null;
    }

    public FindTask(String taskId, String description, CharacterInterface targetNpc) {
        super(taskId, description);
        this.targetPOI = null;
        this.itemId = null;
        this.targetNpc = targetNpc;
    }

    /**
     * @return the POI this task is looking for while it is active, or
     * {@code null} if it is not a location task or is not active.
     */
    public POIInterface getTargetPOI() {
        return TaskStatus.ACTIVE.equals(status) ? targetPOI : null;
    }

    @Override
    public void activate(EventBus eventBus, Quest owningQuest) {
        status = TaskStatus.ACTIVE;

        if (targetPOI != null) {
            locationListener = event -> {
                if (targetPOI.getId().equals(event.getPoiId())) {
                    complete(eventBus, owningQuest);
                }
            };
            eventBus.subscribe(LocationFoundEvent.class, locationListener);
        } else if (itemId != null) {
            itemListener = event -> {
                if (itemId.equals(event.getItemId())) {
                    complete(eventBus, owningQuest);
                }
            };
            eventBus.subscribe(ItemFoundEvent.class, itemListener);
        } else {
            npcListener = event -> {
                if (event.getNpc() == targetNpc) {
                    complete(eventBus, owningQuest);
                }
            };
            eventBus.subscribe(NpcTalkedToEvent.class, npcListener);
        }
    }

    @Override
    public void deactivate(EventBus eventBus) {
        if (locationListener != null) {
            eventBus.unsubscribe(LocationFoundEvent.class, locationListener);
        }
        if (itemListener != null) {
            eventBus.unsubscribe(ItemFoundEvent.class, itemListener);
        }
        if (npcListener != null) {
            eventBus.unsubscribe(NpcTalkedToEvent.class, npcListener);
        }
    }
}
