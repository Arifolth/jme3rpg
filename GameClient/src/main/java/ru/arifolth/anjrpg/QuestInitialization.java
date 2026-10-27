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

import com.jme3.app.Application;
import ru.arifolth.anjrpg.compass.CompassState;
import ru.arifolth.anjrpg.compass.POITarget;
import ru.arifolth.anjrpg.events.EventBus;
import ru.arifolth.anjrpg.events.GameEventListener;
import ru.arifolth.anjrpg.events.TaskActivatedEvent;
import ru.arifolth.anjrpg.events.TaskCompletedEvent;
import ru.arifolth.anjrpg.interfaces.CharacterInterface;
import ru.arifolth.anjrpg.interfaces.GameLogicCoreInterface;
import ru.arifolth.anjrpg.interfaces.compass.POIInterface;
import ru.arifolth.anjrpg.interfaces.compass.POIType;
import ru.arifolth.anjrpg.interfaces.quest.QuestInitializationInterface;
import ru.arifolth.anjrpg.interfaces.quest.QuestServiceInterface;
import ru.arifolth.anjrpg.quest.Quest;
import ru.arifolth.anjrpg.quest.StorySegment;
import ru.arifolth.anjrpg.quest.task.FindTask;
import ru.arifolth.anjrpg.quest.task.KillTask;
import ru.arifolth.anjrpg.quest.task.QuestTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The setup half of the quest system, next to {@code QuestService}'s handling
 * half - the same split as {@code InitializationDelegate} / {@code
 * VegetationInitialization} versus the things they set up. It lives in
 * GameClient because building a quest needs the real world: the NPCs that are
 * standing around, a POI to find, the compass to show it on.
 *
 * <p>Two jobs. It builds quests from definitions and starts them through the
 * {@link QuestServiceInterface}. And it keeps waypoints on the compass in
 * step with the quest: a location task's POI goes onto the compass when the
 * task becomes active ({@link TaskActivatedEvent}) and comes off again when
 * the task is completed ({@link TaskCompletedEvent}). All compass work goes
 * through {@link #onRenderThread}, because the event handlers run on the event
 * bus dispatcher thread.</p>
 *
 * <p>Owned by {@code GameDirector}, reached through {@code
 * GameLogicCoreInterface.getQuestInitialization()}.</p>
 */
public class QuestInitialization implements QuestInitializationInterface {
    final private static Logger LOGGER = Logger.getLogger(QuestInitialization.class.getName());

    private final GameLogicCoreInterface gameLogicCore;
    private final QuestServiceInterface questService;
    private final Map<String, Supplier<Quest>> definitions = new HashMap<>();
    // the waypoint each task put on the compass, so that the same task takes it off again
    private final Map<QuestTask, String> waypoints = new ConcurrentHashMap<>();

    // Field-held listeners (Pattern A), same as SoundManager / KillTracker.
    private final GameEventListener<TaskActivatedEvent> taskActivatedListener = this::onTaskActivated;
    private final GameEventListener<TaskCompletedEvent> taskCompletedListener = this::onTaskCompleted;

    public QuestInitialization(GameLogicCoreInterface gameLogicCore, QuestServiceInterface questService) {
        this.gameLogicCore = gameLogicCore;
        this.questService = questService;

        EventBus eventBus = gameLogicCore.getEventBus();
        eventBus.subscribe(TaskActivatedEvent.class, taskActivatedListener);
        eventBus.subscribe(TaskCompletedEvent.class, taskCompletedListener);

        definitions.put("test_quest", this::createTestQuest);
    }

    @Override
    public void startQuest(String questId) {
        Supplier<Quest> definition = definitions.get(questId);
        if (definition == null) {
            LOGGER.log(Level.WARNING, "Unknown quest: " + questId);
            return;
        }

        Quest quest = definition.get();
        if (quest != null) {
            questService.startQuest(quest);
        }
    }

    /**
     * Throwaway, hardcoded quest so the whole loop (events -> tasks -> journal ->
     * compass) can be exercised before any quest-giver system exists. Segment 1:
     * kill up to three of the NPCs standing around right now; segment 2: walk to
     * a waystone 300 units from the start. Remove once quests are started by NPC
     * dialogue.
     */
    private Quest createTestQuest() {
        Set<CharacterInterface> targets = new HashSet<>();
        for (CharacterInterface npc : gameLogicCore.getCharacterMap().values()) {
            if (targets.size() < 3) {
                targets.add(npc);
            }
        }

        if (targets.isEmpty()) {
            LOGGER.log(Level.INFO, "No NPCs present - test quest not started");
            return null;
        }

        POITarget waystone = new POITarget("test_waystone",
                gameLogicCore.getPlayerCharacter().getCharacterControl().getPhysicsLocation().add(300f, 0f, 0),
                "Old Waystone", POIType.LANDMARK);
        LOGGER.log(Level.INFO, "Test quest waystone at " + waystone.getPosition());

        return new Quest("test_quest", "Test Quest", List.of(
                new StorySegment("Clear the way", "Hostile creatures roam nearby. Defeat them.")
                        .addRequiredTask(new KillTask("test_kill", "defeat " + targets.size() + " hostile creatures", targets)),
                new StorySegment("The waystone", "Find the Old Waystone.")
                        .addRequiredTask(new FindTask("test_find", "find the Old Waystone", waystone))
        ));
    }

    private void onTaskActivated(TaskActivatedEvent event) {
        if (!(event.getTask() instanceof FindTask)) {
            return;
        }

        // only location tasks have a POI, and only while they are active - which they now are
        POIInterface poi = ((FindTask) event.getTask()).getTargetPOI();
        if (poi == null) {
            return;
        }

        waypoints.put(event.getTask(), poi.getId());
        onRenderThread(compassState -> compassState.addPOI(poi));
    }

    private void onTaskCompleted(TaskCompletedEvent event) {
        String poiId = waypoints.remove(event.getTask());
        if (poiId != null) {
            onRenderThread(compassState -> compassState.removePOI(poiId));
        }
    }

    /**
     * The event handlers run on the event bus dispatcher thread; the compass
     * attaches and detaches scene graph nodes, so its work is handed over to the
     * render thread. Tasks run in the order they were enqueued, so a waypoint
     * that is added and then removed is never removed before it exists.
     */
    private void onRenderThread(Consumer<CompassState> action) {
        Application app = gameLogicCore.getApp();
        app.enqueue(() -> {
            CompassState compassState = app.getStateManager().getState(CompassState.class);
            if (compassState == null) {
                LOGGER.log(Level.WARNING, "No compass to put the quest waypoint on");
                return;
            }
            action.accept(compassState);
        });
    }
}
