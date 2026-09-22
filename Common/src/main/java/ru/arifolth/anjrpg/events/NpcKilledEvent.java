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

import com.jme3.math.Vector3f;
import ru.arifolth.anjrpg.interfaces.CharacterInterface;

/**
 * Published by {@code NonPlayerCharacter.die()} once an NPC's death has been
 * committed (health reached zero and {@code die()} has run).
 *
 * <p>{@code npc} is the character that died - treat it as read-only, per
 * the {@link GameEvent} contract; do not call mutating methods on it from a
 * listener. {@code location} is a defensive copy of its death position, so
 * it is safe for a subscriber to keep or mutate freely.</p>
 *
 * <p>This intentionally does not carry an "attacker"/killer reference: at
 * the current call site ({@code HealthBar.applyDamage} -&gt; {@code die()})
 * that information was never threaded through, and every caller of {@code
 * applyDamage(float)} (both {@code PlayerCharacter} and {@code
 * NonPlayerCharacter}) would need a signature change to supply it. That is
 * a reasonable follow-up if a listener ever needs "who got the kill", but
 * is out of scope here - see the accompanying note.</p>
 */
public final class NpcKilledEvent implements GameEvent {

    private final CharacterInterface npc;
    private final Vector3f location;

    public NpcKilledEvent(CharacterInterface npc, Vector3f location) {
        this.npc = npc;
        this.location = location.clone();
    }

    public CharacterInterface getNpc() {
        return npc;
    }

    public Vector3f getLocation() {
        return location.clone();
    }

    @Override
    public String toString() {
        return "NpcKilledEvent{" + npc.getNode().getName() + " at " + location + "}";
    }
}
