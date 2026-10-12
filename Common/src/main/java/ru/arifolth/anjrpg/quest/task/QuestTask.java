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

package ru.arifolth.anjrpg.quest.task;

import ru.arifolth.anjrpg.events.EventBus;
import ru.arifolth.anjrpg.events.TaskCompletedEvent;
import ru.arifolth.anjrpg.quest.Quest;

/**
 * A single objective of a {@link Quest}. Push-based: an active task
 * subscribes to exactly the event type that can fulfil it, and reports its
 * own completion through {@link #complete}. Nothing polls it.
 */
public abstract class QuestTask {
    protected volatile TaskStatus status = TaskStatus.PENDING;
    private final String taskId;
    private final String description;

    protected QuestTask(String taskId, String description) {
        this.taskId = taskId;
        this.description = description;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getDescription() {
        return description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    /** Called once by the owning quest when this task becomes one of the active ones. */
    public abstract void activate(EventBus eventBus, Quest owningQuest);

    /** Called when the task completes, or if the quest/task is abandoned before completion. */
    public abstract void deactivate(EventBus eventBus);

    /**
     * A task marks itself complete, stops listening, and tells the bus; the
     * owning {@link Quest} reacts to the {@link TaskCompletedEvent}. Safe to
     * call more than once - only the first call has any effect.
     */
    protected void complete(EventBus eventBus, Quest owningQuest) {
        if (TaskStatus.COMPLETED.equals(status)) {
            return;
        }
        status = TaskStatus.COMPLETED;
        deactivate(eventBus);
        eventBus.publish(new TaskCompletedEvent(owningQuest, this));
    }
}
