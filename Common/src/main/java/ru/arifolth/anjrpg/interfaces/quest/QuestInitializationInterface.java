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

package ru.arifolth.anjrpg.interfaces.quest;

/**
 * The setup half of the quest system: builds quests out of the real game
 * world (picks the NPCs, creates the waypoints), keeps their waypoints on the
 * compass while the matching task is active, and hands the finished quest to
 * the {@link QuestServiceInterface}. Whatever starts a quest - the game at
 * start-up today, a quest-giver NPC's dialogue tomorrow - asks for it here by
 * id and does not need to know about the compass or the world.
 */
public interface QuestInitializationInterface {
    /**
     * Builds the quest with the given id and starts it. Unknown ids, and
     * quests that cannot be built right now (for example no NPCs present to
     * kill), are logged and ignored.
     */
    void startQuest(String questId);
}
