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
import ru.arifolth.anjrpg.events.ItemDeliveredEvent;
import ru.arifolth.anjrpg.interfaces.compass.POIInterface;
import ru.arifolth.anjrpg.quest.Quest;

/**
 * Deliver a Quest Item to a destination POI. Completes on an
 * {@link ItemDeliveredEvent} carrying this task's item id and the
 * destination POI's id.
 */
public class DeliverTask extends QuestTask {
    private final String itemId;
    private final POIInterface destinationPOI;

    private GameEventListener<ItemDeliveredEvent> listener;

    public DeliverTask(String taskId, String description, String itemId, POIInterface destinationPOI) {
        super(taskId, description);
        this.itemId = itemId;
        this.destinationPOI = destinationPOI;
    }

    @Override
    public void activate(EventBus eventBus, Quest owningQuest) {
        status = TaskStatus.ACTIVE;
        listener = event -> {
            if (itemId.equals(event.getItemId()) && destinationPOI.getId().equals(event.getPoiId())) {
                complete(eventBus, owningQuest);
            }
        };
        eventBus.subscribe(ItemDeliveredEvent.class, listener);
    }

    @Override
    public void deactivate(EventBus eventBus) {
        if (listener != null) {
            eventBus.unsubscribe(ItemDeliveredEvent.class, listener);
        }
    }
}
