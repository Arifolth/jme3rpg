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

package ru.arifolth.anjrpg.quest;

import ru.arifolth.anjrpg.events.EventBus;
import ru.arifolth.anjrpg.events.GameEventListener;
import ru.arifolth.anjrpg.events.QuestCompletedEvent;
import ru.arifolth.anjrpg.events.QuestStartedEvent;
import ru.arifolth.anjrpg.events.TaskCompletedEvent;
import ru.arifolth.anjrpg.interfaces.compass.POIInterface;
import ru.arifolth.anjrpg.interfaces.quest.JournalServiceInterface;
import ru.arifolth.anjrpg.interfaces.quest.QuestServiceInterface;
import ru.arifolth.anjrpg.quest.task.FindTask;
import ru.arifolth.anjrpg.quest.task.QuestTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The single entry point for quests: start them, look them up, and route
 * task completions to the quest that owns the task. The fulfilment logic
 * itself stays distributed - each {@link QuestTask} subscribes to the one
 * event type that can fulfil it - exactly as {@code VegetationInitialization}
 * assembles distributed builders without containing their placement logic.
 *
 * <p>Owned by {@code GameDirector}, reached through
 * {@code GameLogicCoreInterface.getQuestService()} - no static access.</p>
 */
public class QuestService implements QuestServiceInterface {
    private final EventBus eventBus;
    private final JournalServiceInterface journalService;
    private final List<Quest> activeQuests = new CopyOnWriteArrayList<>();
    private final List<Quest> completedQuests = new CopyOnWriteArrayList<>();

    // Field-held listeners (Pattern A), same as SoundManager / KillTracker.
    private final GameEventListener<TaskCompletedEvent> taskCompletedListener = this::onTaskCompleted;
    private final GameEventListener<QuestCompletedEvent> questCompletedListener = this::onQuestCompleted;

    public QuestService(EventBus eventBus, JournalServiceInterface journalService) {
        this.eventBus = eventBus;
        this.journalService = journalService;

        eventBus.subscribe(TaskCompletedEvent.class, taskCompletedListener);
        eventBus.subscribe(QuestCompletedEvent.class, questCompletedListener);
    }

    @Override
    public void startQuest(Quest quest) {
        if (activeQuests.contains(quest) || completedQuests.contains(quest)) {
            return;
        }

        // registered before it starts, so a task completing immediately can find its quest
        activeQuests.add(quest);
        if (quest.start(eventBus)) {
            eventBus.publish(new QuestStartedEvent(quest));
        } else {
            activeQuests.remove(quest);
        }
    }

    @Override
    public List<Quest> getActiveQuests() {
        return new ArrayList<>(activeQuests);
    }

    @Override
    public List<Quest> getCompletedQuests() {
        return new ArrayList<>(completedQuests);
    }

    @Override
    public List<POIInterface> getActiveLocationTargets() {
        List<POIInterface> targets = new ArrayList<>();
        for (Quest quest : activeQuests) {
            StorySegment segment = quest.getCurrentSegment();
            if (segment == null) {
                continue;
            }
            for (QuestTask task : segment.getRequiredTasks()) {
                if (task instanceof FindTask) {
                    POIInterface poi = ((FindTask) task).getTargetPOI();
                    if (poi != null) {
                        targets.add(poi);
                    }
                }
            }
        }
        return targets;
    }

    private void onTaskCompleted(TaskCompletedEvent event) {
        Quest quest = event.getQuest();
        if (activeQuests.contains(quest)) {
            quest.onTaskCompleted(event.getTask(), eventBus, journalService);
        }
    }

    private void onQuestCompleted(QuestCompletedEvent event) {
        Quest quest = event.getQuest();
        if (activeQuests.remove(quest)) {
            completedQuests.add(quest);
        }
    }
}
