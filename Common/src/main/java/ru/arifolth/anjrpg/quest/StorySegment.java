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

import ru.arifolth.anjrpg.quest.task.QuestTask;
import ru.arifolth.anjrpg.quest.task.TaskStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One chapter of a {@link Quest}: a title and description for the journal
 * plus the tasks that must all be completed before the quest moves on.
 */
public class StorySegment {
    private final String title;
    private final String description;
    private final List<QuestTask> requiredTasks = new ArrayList<>();

    public StorySegment(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public StorySegment addRequiredTask(QuestTask task) {
        requiredTasks.add(task);
        return this;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public List<QuestTask> getRequiredTasks() {
        return Collections.unmodifiableList(requiredTasks);
    }

    public boolean isCompleted() {
        for (QuestTask task : requiredTasks) {
            if (!TaskStatus.COMPLETED.equals(task.getStatus())) {
                return false;
            }
        }
        return true;
    }
}
