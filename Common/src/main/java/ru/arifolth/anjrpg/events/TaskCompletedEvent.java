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

import ru.arifolth.anjrpg.quest.Quest;
import ru.arifolth.anjrpg.quest.task.QuestTask;

/**
 * Published by a {@link QuestTask} itself once its own completion condition
 * is met. The owning {@link Quest} reacts to it (via {@code QuestService})
 * to advance its story.
 */
public final class TaskCompletedEvent implements GameEvent {

    private final Quest quest;
    private final QuestTask task;

    public TaskCompletedEvent(Quest quest, QuestTask task) {
        this.quest = quest;
        this.task = task;
    }

    public Quest getQuest() {
        return quest;
    }

    public QuestTask getTask() {
        return task;
    }

    @Override
    public String toString() {
        return "TaskCompletedEvent{" + quest.getQuestId() + "/" + task.getTaskId() + "}";
    }
}
