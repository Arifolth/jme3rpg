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
 * The player talked to an NPC. Pure event definition for now: nothing in the
 * game publishes it yet - the future peaceful-NPC dialogue system will.
 */
public final class NpcTalkedToEvent implements GameEvent {

    private final CharacterInterface npc;
    private final CharacterInterface talker;

    public NpcTalkedToEvent(CharacterInterface npc, CharacterInterface talker) {
        this.npc = npc;
        this.talker = talker;
    }

    public CharacterInterface getNpc() {
        return npc;
    }

    public CharacterInterface getTalker() {
        return talker;
    }

    @Override
    public String toString() {
        return "NpcTalkedToEvent{" + npc.getNode().getName() + "}";
    }
}
