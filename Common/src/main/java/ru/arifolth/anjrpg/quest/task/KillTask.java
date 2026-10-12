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
import ru.arifolth.anjrpg.events.NpcKilledEvent;
import ru.arifolth.anjrpg.interfaces.CharacterInterface;
import ru.arifolth.anjrpg.quest.Quest;

import java.util.HashSet;
import java.util.Set;

/**
 * Kill one or more exact NPCs, matched by identity against the {@code npc}
 * carried by {@link NpcKilledEvent}. One target = "kill this boss"; several =
 * "kill all of these". Type-based "kill N of type X" is deliberately not
 * supported yet - there is no enemy-type system to match against.
 */
public class KillTask extends QuestTask {
    private final Set<CharacterInterface> remainingTargets; // only touched on the bus dispatcher thread

    private GameEventListener<NpcKilledEvent> listener;

    public KillTask(String taskId, String description, Set<CharacterInterface> targets) {
        super(taskId, description);
        this.remainingTargets = new HashSet<>(targets);
    }

    @Override
    public void activate(EventBus eventBus, Quest owningQuest) {
        status = TaskStatus.ACTIVE;
        listener = event -> onNpcKilled(event, eventBus, owningQuest);
        eventBus.subscribe(NpcKilledEvent.class, listener);
    }

    @Override
    public void deactivate(EventBus eventBus) {
        if (listener != null) {
            eventBus.unsubscribe(NpcKilledEvent.class, listener);
        }
    }

    private void onNpcKilled(NpcKilledEvent event, EventBus eventBus, Quest owningQuest) {
        if (!remainingTargets.remove(event.getNpc())) {
            return; // not one of ours
        }

        if (remainingTargets.isEmpty()) {
            complete(eventBus, owningQuest);
        }
    }
}
