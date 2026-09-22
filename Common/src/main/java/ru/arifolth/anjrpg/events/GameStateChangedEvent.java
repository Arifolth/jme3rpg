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

package ru.arifolth.anjrpg.events;

import ru.arifolth.anjrpg.interfaces.GameState;

/**
 * Published by {@code GameStateManager.changeState} after a transition has
 * been committed. {@code from} is the previous state, {@code to} is the new
 * current state.
 */
public final class GameStateChangedEvent implements GameEvent {

    private final GameState from;
    private final GameState to;

    public GameStateChangedEvent(GameState from, GameState to) {
        this.from = from;
        this.to = to;
    }

    public GameState getFrom() {
        return from;
    }

    public GameState getTo() {
        return to;
    }

    @Override
    public String toString() {
        return "GameStateChangedEvent{" + from + " -> " + to + "}";
    }
}
