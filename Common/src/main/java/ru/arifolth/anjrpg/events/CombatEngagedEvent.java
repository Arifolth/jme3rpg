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

import ru.arifolth.anjrpg.interfaces.CharacterInterface;

/**
 * Published by {@code NonPlayerCharacter} on the transition from "not
 * attacking" to "attacking" the player - edge-triggered, not once per
 * attack. {@code attacker} is the NPC that has just engaged; treat it as
 * read-only, per the {@link GameEvent} contract.
 */
public final class CombatEngagedEvent implements GameEvent {

    private final CharacterInterface attacker;

    public CombatEngagedEvent(CharacterInterface attacker) {
        this.attacker = attacker;
    }

    public CharacterInterface getAttacker() {
        return attacker;
    }

    @Override
    public String toString() {
        return "CombatEngagedEvent{" + attacker.getNode().getName() + "}";
    }
}
