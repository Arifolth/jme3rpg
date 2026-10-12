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
import ru.arifolth.anjrpg.events.JournalEntryAddedEvent;
import ru.arifolth.anjrpg.events.QuestCompletedEvent;
import ru.arifolth.anjrpg.events.QuestStartedEvent;
import ru.arifolth.anjrpg.interfaces.quest.JournalServiceInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The player's journal: the authoritative store of {@link JournalEntry}s.
 * Owned by {@code GameDirector}, reached through
 * {@code GameLogicCoreInterface.getJournalService()} - no static access.
 *
 * <p>It writes the "Start" and "Completed" entries itself in response to
 * {@link QuestStartedEvent} / {@link QuestCompletedEvent}; every other entry
 * is created by the {@link Quest} and handed in through {@link #addEntry}.
 * Each stored entry is announced with a {@link JournalEntryAddedEvent}.</p>
 */
public class JournalService implements JournalServiceInterface {
    private final EventBus eventBus;
    private final List<JournalEntry> entries = new CopyOnWriteArrayList<>();

    // Field-held listeners (Pattern A), same as SoundManager / KillTracker.
    private final GameEventListener<QuestStartedEvent> startedListener = this::onQuestStarted;
    private final GameEventListener<QuestCompletedEvent> completedListener = this::onQuestCompleted;

    public JournalService(EventBus eventBus) {
        this.eventBus = eventBus;

        eventBus.subscribe(QuestStartedEvent.class, startedListener);
        eventBus.subscribe(QuestCompletedEvent.class, completedListener);
    }

    @Override
    public void addEntry(JournalEntry entry) {
        entries.add(entry);
        eventBus.publish(new JournalEntryAddedEvent(entry));
    }

    @Override
    public List<JournalEntry> getAllEntries() {
        return new ArrayList<>(entries);
    }

    @Override
    public List<JournalEntry> getEntriesForQuest(String questId) {
        return entries.stream()
                .filter(e -> e.getQuestId().equals(questId))
                .toList();
    }

    private void onQuestStarted(QuestStartedEvent event) {
        Quest quest = event.getQuest();
        addEntry(new JournalEntry(quest.getQuestId(), quest.getTitle() + " - Start", "You have begun the quest."));

        StorySegment first = quest.getCurrentSegment();
        if (first != null) {
            addEntry(new JournalEntry(quest.getQuestId(), quest.getTitle() + " - " + first.getTitle(), first.getDescription()));
        }
    }

    private void onQuestCompleted(QuestCompletedEvent event) {
        Quest quest = event.getQuest();
        addEntry(new JournalEntry(quest.getQuestId(), quest.getTitle() + " - Completed", "Quest completed."));
    }
}
