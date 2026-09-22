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

import ru.arifolth.anjrpg.events.EventBus;
import ru.arifolth.anjrpg.events.GameEventListener;
import ru.arifolth.anjrpg.events.NpcKilledEvent;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Counts NPC kills as {@link NpcKilledEvent}s arrive on the {@link EventBus}.
 *
 * <p>This is deliberately small: a working example of "subscribe to an
 * event and handle it" per the original request, not a real progression /
 * achievements / quest system. {@link #getKillCount()} is a plain read -
 * wire it into a HUD, a quest tracker, save data, whatever the game
 * actually needs next; the wiring pattern (constructor-subscribe, field-
 * held listener, plain getter) is the reusable part.</p>
 *
 * <p>The listener runs on the bus's single dispatcher thread, so the
 * increment itself never races. {@link AtomicInteger} is used only so
 * {@link #getKillCount()} has a well-defined, visible value when read from
 * a different thread (e.g. the render thread, for a HUD).</p>
 */
public class KillTracker {
    final private static Logger LOGGER = Logger.getLogger(KillTracker.class.getName());

    private final AtomicInteger killCount = new AtomicInteger(0);
    private final EventBus eventBus;

    // Field-held listener (Pattern A): required so it can be unsubscribed,
    // e.g. if a KillTracker is ever replaced or torn down independently of
    // the bus itself. An inline lambda passed straight to subscribe()
    // could not be unsubscribed later.
    private final GameEventListener<NpcKilledEvent> killedListener = this::onNpcKilled;

    public KillTracker(EventBus eventBus) {
        this.eventBus = eventBus;
        eventBus.subscribe(NpcKilledEvent.class, killedListener);
    }

    public void detach() {
        eventBus.unsubscribe(NpcKilledEvent.class, killedListener);
    }

    private void onNpcKilled(NpcKilledEvent event) {
        int total = killCount.incrementAndGet();
        LOGGER.log(Level.INFO, "NPC killed: " + event.getNpc().getNode().getName() + " (total kills: " + total + ")");
    }

    public int getKillCount() {
        return killCount.get();
    }
}
