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
import ru.arifolth.anjrpg.interfaces.CombatTracker;
import ru.arifolth.anjrpg.interfaces.CombatTrackerInterface;
import ru.arifolth.anjrpg.interfaces.GameDirectorInterface;
import ru.arifolth.anjrpg.interfaces.GameStateManagerInterface;
import ru.arifolth.anjrpg.interfaces.quest.JournalServiceInterface;
import ru.arifolth.anjrpg.interfaces.quest.QuestServiceInterface;
import ru.arifolth.anjrpg.quest.JournalService;
import ru.arifolth.anjrpg.quest.QuestService;

/**
 * Constructs and composes the focused game-flow subsystems rather than any of
 * them absorbing another: {@code CombatTracker} stays about combat
 * engagement, {@code QuestService} stays about quests. Mirrors how
 * {@code VegetationInitialization} assembles its distributed builders.
 *
 * <p>{@code GameLogicCore} owns the one instance and delegates its existing
 * getters here, so callers keep going through {@code GameLogicCoreInterface}.</p>
 */
public class GameDirector implements GameDirectorInterface {
    private final CombatTracker combatTracker;
    private final JournalService journalService;
    private final QuestService questService;

    public GameDirector(EventBus eventBus, GameStateManagerInterface gameStateManager) {
        this.combatTracker = new CombatTracker(gameStateManager, eventBus);
        this.journalService = new JournalService(eventBus);
        this.questService = new QuestService(eventBus, journalService);
        // future: NpcPopulationService, LandscapeDirector construct and plug in here,
        // once either has a defined shape - nothing above this line needs to change
        // when that happens
    }

    @Override
    public CombatTrackerInterface getCombatTracker() {
        return combatTracker;
    }

    @Override
    public QuestServiceInterface getQuestService() {
        return questService;
    }

    @Override
    public JournalServiceInterface getJournalService() {
        return journalService;
    }
}
