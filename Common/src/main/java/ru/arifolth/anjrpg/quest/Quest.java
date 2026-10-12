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
import ru.arifolth.anjrpg.events.QuestCompletedEvent;
import ru.arifolth.anjrpg.interfaces.quest.JournalServiceInterface;
import ru.arifolth.anjrpg.quest.task.QuestTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An ordered list of {@link StorySegment}s. Tasks of the current segment are
 * active (subscribed to the bus) all at once; when the last of them reports
 * completion, the quest moves on to the next segment, or completes.
 *
 * <p>{@link #start} and {@link #onTaskCompleted} are package-private on
 * purpose: only {@code QuestService} drives a quest. They are synchronized
 * because a quest is started from the game thread while task completions
 * arrive on the event bus dispatcher thread. A quest must have at least one
 * segment, and every segment at least one task - otherwise it could never
 * advance.</p>
 */
public class Quest {
    final private static Logger LOGGER = Logger.getLogger(Quest.class.getName());

    private final String questId;
    private final String title;
    private final List<StorySegment> storyPath;
    private volatile QuestState state = QuestState.PENDING;
    private volatile int currentSegmentIndex = 0;

    public Quest(String questId, String title, List<StorySegment> storyPath) {
        if (storyPath.isEmpty()) {
            throw new IllegalArgumentException("Quest " + questId + " has no story segments");
        }
        for (StorySegment segment : storyPath) {
            if (segment.getRequiredTasks().isEmpty()) {
                throw new IllegalArgumentException("Segment '" + segment.getTitle() + "' of quest " + questId + " has no required tasks and could never advance");
            }
        }
        this.questId = questId;
        this.title = title;
        this.storyPath = new ArrayList<>(storyPath);
    }

    public String getQuestId() {
        return questId;
    }

    public String getTitle() {
        return title;
    }

    public QuestState getState() {
        return state;
    }

    public List<StorySegment> getStoryPath() {
        return Collections.unmodifiableList(storyPath);
    }

    /**
     * @return the segment currently being worked on, or {@code null} if the
     * quest has no segments / has run past the last one.
     */
    public StorySegment getCurrentSegment() {
        int index = currentSegmentIndex;
        return index < storyPath.size() ? storyPath.get(index) : null;
    }

    public boolean isCompleted() {
        return QuestState.COMPLETED.equals(state);
    }

    /**
     * Activates the first segment's tasks. {@code QuestService} announces the
     * start ({@code QuestStartedEvent}) itself, right after this returns.
     *
     * @return {@code false} if the quest had already been started
     */
    synchronized boolean start(EventBus eventBus) {
        if (!QuestState.PENDING.equals(state)) {
            return false;
        }

        LOGGER.log(Level.INFO, "Quest started: " + questId);
        state = QuestState.ACTIVE;
        activateCurrentSegment(eventBus);
        return true;
    }

    /**
     * Called by {@code QuestService} on a {@code TaskCompletedEvent} for one
     * of this quest's tasks.
     */
    synchronized void onTaskCompleted(QuestTask task, EventBus eventBus, JournalServiceInterface journal) {
        StorySegment segment = getCurrentSegment();
        if (!QuestState.ACTIVE.equals(state) || segment == null || !segment.getRequiredTasks().contains(task)) {
            return;
        }

        journal.addEntry(new JournalEntry(questId, title + " - Task Completed", "Task " + task.getDescription() + " completed."));

        if (!segment.isCompleted()) {
            return;
        }

        currentSegmentIndex++;
        StorySegment next = getCurrentSegment();
        if (next == null) {
            LOGGER.log(Level.INFO, "Quest completed: " + questId);
            state = QuestState.COMPLETED;
            eventBus.publish(new QuestCompletedEvent(this));
        } else {
            journal.addEntry(new JournalEntry(questId, title + " - " + next.getTitle(), next.getDescription()));
            activateCurrentSegment(eventBus);
        }
    }

    private void activateCurrentSegment(EventBus eventBus) {
        for (QuestTask task : getCurrentSegment().getRequiredTasks()) {
            task.activate(eventBus, this);
        }
    }
}
