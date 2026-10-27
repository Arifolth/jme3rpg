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
 * Published by a {@link Quest} when one of its tasks becomes active - when the
 * quest starts for the tasks of its first segment, and again whenever a
 * completed segment hands over to the next. The counterpart of
 * {@link TaskCompletedEvent}; it is what lets quest setup react to "this
 * objective is now live" (for example, put its waypoint on the compass).
 */
public final class TaskActivatedEvent implements GameEvent {

    private final Quest quest;
    private final QuestTask task;

    public TaskActivatedEvent(Quest quest, QuestTask task) {
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
        return "TaskActivatedEvent{" + quest.getQuestId() + "/" + task.getTaskId() + "}";
    }
}
