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

import com.jme3.math.Vector3f;
import ru.arifolth.anjrpg.compass.POITarget;
import ru.arifolth.anjrpg.events.EventBus;
import ru.arifolth.anjrpg.events.GameEvent;
import ru.arifolth.anjrpg.events.GameEventListener;
import ru.arifolth.anjrpg.events.ItemDeliveredEvent;
import ru.arifolth.anjrpg.events.ItemFoundEvent;
import ru.arifolth.anjrpg.events.JournalEntryAddedEvent;
import ru.arifolth.anjrpg.events.LocationFoundEvent;
import ru.arifolth.anjrpg.events.NpcKilledEvent;
import ru.arifolth.anjrpg.events.NpcTalkedToEvent;
import ru.arifolth.anjrpg.events.TaskActivatedEvent;
import ru.arifolth.anjrpg.interfaces.CharacterInterface;
import ru.arifolth.anjrpg.interfaces.compass.POIType;
import ru.arifolth.anjrpg.quest.task.DeliverTask;
import ru.arifolth.anjrpg.quest.task.FindTask;
import ru.arifolth.anjrpg.quest.task.KillTask;
import ru.arifolth.anjrpg.quest.task.TaskStatus;

import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;

/**
 * Manual smoke test for the quest, task and journal code, in
 * the same style as {@code EventBusTest}: no JUnit dependency exists in this
 * project, so this is a plain class with a {@code main()}; every check is a
 * no-arg method that throws {@link AssertionError} on failure.
 *
 * <p>Everything runs against a real {@link EventBus} (started the same way
 * {@code EventBusTest} does) and the real services; the only fake is a
 * {@link CharacterInterface} proxy (identity is all the code under test uses).
 * The combat tracker has its own test, {@code CombatTrackerTest}.</p>
 *
 * <p>Run from an IDE (run {@code main}), or from the command line:
 * {@code mvn -pl GameClient test-compile exec:java
 * -Dexec.classpathScope=test -Dexec.mainClass=ru.arifolth.anjrpg.quest.QuestMechanicsTest}</p>
 */
public class QuestMechanicsTest {

    public static void main(String[] args) {
        Runnable[] checks = {
                QuestMechanicsTest::testKillThenFindQuestEndToEnd,
                QuestMechanicsTest::testParallelTasksFindNpcItemAndDeliver,
                QuestMechanicsTest::testNonMatchingEventsAreIgnored,
                QuestMechanicsTest::testQuestValidationAndDoubleStart
        };
        String[] names = {
                "quest: kill segment then find-location segment, journal and completion",
                "quest: find item + talk to NPC + deliver item in one segment, any order",
                "quest: non-matching events do not complete tasks",
                "quest: empty quests rejected, double start ignored"
        };

        int passed = 0;
        for (int i = 0; i < checks.length; i++) {
            try {
                checks[i].run();
                System.out.println("PASS: " + names[i]);
                passed++;
            } catch (Throwable t) {
                System.out.println("FAIL: " + names[i] + " -> " + t);
            }
        }

        System.out.println(passed + "/" + checks.length + " checks passed");
        if (passed != checks.length) {
            System.exit(1);
        }
    }

    // -------------------------------------------------------------- quests

    private static void testKillThenFindQuestEndToEnd() {
        EventBus bus = newStartedBus();
        JournalService journal = new JournalService(bus);
        QuestService quests = new QuestService(bus, journal);

        CharacterInterface boss1 = fakeCharacter("boss1");
        CharacterInterface boss2 = fakeCharacter("boss2");
        CharacterInterface bystander = fakeCharacter("bystander");
        POITarget waystone = new POITarget("waystone", new Vector3f(10, 0, 10), "Old Waystone", POIType.LANDMARK);

        List<JournalEntry> announced = new CopyOnWriteArrayList<>();
        bus.subscribe(JournalEntryAddedEvent.class, (GameEventListener<JournalEntryAddedEvent>) e -> announced.add(e.getEntry()));
        List<String> activated = new CopyOnWriteArrayList<>();
        bus.subscribe(TaskActivatedEvent.class, (GameEventListener<TaskActivatedEvent>) e -> activated.add(e.getTask().getTaskId()));

        KillTask kill = new KillTask("kill", "kill both bosses", Set.of(boss1, boss2));
        FindTask find = new FindTask("find", "find the waystone", waystone);
        Quest quest = new Quest("q1", "Quest One", List.of(
                new StorySegment("Fight", "Kill them.").addRequiredTask(kill),
                new StorySegment("Walk", "Find it.").addRequiredTask(find)));

        quests.startQuest(quest);
        waitUntil(() -> activated.equals(List.of("kill")), "only the first segment's task may be announced as activated at start, got " + activated);
        assertEquals(QuestState.ACTIVE, quest.getState(), "quest should be ACTIVE after start");
        assertEquals(1, quests.getActiveQuests().size(), "one active quest");
        assertTrue(quests.getActiveLocationTargets().isEmpty(), "find task is not active yet, so no location target");
        waitUntil(() -> journal.getAllEntries().size() == 2, "Start + first segment entries were not written");

        bus.publish(new NpcKilledEvent(bystander, new Vector3f()));
        bus.publish(new NpcKilledEvent(boss1, new Vector3f()));
        flush(bus);
        assertEquals(TaskStatus.ACTIVE, kill.getStatus(), "kill task needs both targets");

        bus.publish(new NpcKilledEvent(boss2, new Vector3f()));
        waitUntil(() -> TaskStatus.COMPLETED.equals(kill.getStatus()), "kill task did not complete");
        waitUntil(() -> TaskStatus.ACTIVE.equals(find.getStatus()), "second segment's find task was not activated");
        waitUntil(() -> activated.equals(List.of("kill", "find")), "second segment's task was not announced as activated, got " + activated);
        assertEquals(1, quests.getActiveLocationTargets().size(), "waystone should now be a location target");
        assertEquals("waystone", quests.getActiveLocationTargets().get(0).getId(), "target should be the waystone");

        bus.publish(new LocationFoundEvent("somewhere else", fakeCharacter("player")));
        flush(bus);
        assertEquals(QuestState.ACTIVE, quest.getState(), "wrong location must not complete the quest");

        bus.publish(new LocationFoundEvent("waystone", fakeCharacter("player")));
        waitUntil(quest::isCompleted, "quest did not complete");
        waitUntil(() -> quests.getCompletedQuests().size() == 1 && quests.getActiveQuests().isEmpty(), "service did not move the quest to completed");
        assertTrue(quests.getActiveLocationTargets().isEmpty(), "no location targets once the quest is done");

        waitUntil(() -> announced.size() == journal.getAllEntries().size() && announced.size() == 6,
                "expected 6 journal entries (start, first segment, task, second segment, task, completed) - got " + journal.getAllEntries());
        assertEquals(journal.getAllEntries().size(), journal.getEntriesForQuest("q1").size(), "every entry belongs to q1");
        assertEquals("Quest One - Completed", journal.getAllEntries().get(journal.getAllEntries().size() - 1).getTitle(), "last entry should be the completion entry");
    }

    private static void testParallelTasksFindNpcItemAndDeliver() {
        EventBus bus = newStartedBus();
        JournalService journal = new JournalService(bus);
        QuestService quests = new QuestService(bus, journal);

        CharacterInterface elder = fakeCharacter("elder");
        CharacterInterface player = fakeCharacter("player");
        POITarget shrine = new POITarget("shrine", new Vector3f(), "Shrine", POIType.LANDMARK);

        FindTask findItem = new FindTask("find_item", "find the sigil", "sigil");
        FindTask findNpc = new FindTask("find_npc", "talk to the elder", elder);
        DeliverTask deliver = new DeliverTask("deliver", "deliver the sigil", "sigil", shrine);
        Quest quest = new Quest("q2", "Quest Two", List.of(
                new StorySegment("Errand", "Do all three.").addRequiredTask(findItem).addRequiredTask(findNpc).addRequiredTask(deliver)));
        quests.startQuest(quest);

        bus.publish(new NpcTalkedToEvent(elder, player));
        bus.publish(new ItemFoundEvent("sigil", player));
        waitUntil(() -> TaskStatus.COMPLETED.equals(findNpc.getStatus()) && TaskStatus.COMPLETED.equals(findItem.getStatus()), "find tasks did not complete");
        assertEquals(QuestState.ACTIVE, quest.getState(), "quest needs the delivery too");

        bus.publish(new ItemDeliveredEvent("sigil", "shrine", player));
        waitUntil(quest::isCompleted, "quest did not complete after the last task");
    }

    private static void testNonMatchingEventsAreIgnored() {
        EventBus bus = newStartedBus();
        JournalService journal = new JournalService(bus);
        QuestService quests = new QuestService(bus, journal);

        CharacterInterface elder = fakeCharacter("elder");
        CharacterInterface stranger = fakeCharacter("stranger");
        CharacterInterface player = fakeCharacter("player");
        POITarget shrine = new POITarget("shrine", new Vector3f(), "Shrine", POIType.LANDMARK);

        FindTask findItem = new FindTask("find_item", "find the sigil", "sigil");
        FindTask findNpc = new FindTask("find_npc", "talk to the elder", elder);
        DeliverTask deliver = new DeliverTask("deliver", "deliver the sigil", "sigil", shrine);
        Quest quest = new Quest("q3", "Quest Three", List.of(
                new StorySegment("Errand", "Do all three.").addRequiredTask(findItem).addRequiredTask(findNpc).addRequiredTask(deliver)));
        quests.startQuest(quest);

        bus.publish(new ItemFoundEvent("not the sigil", player));
        bus.publish(new NpcTalkedToEvent(stranger, player));
        bus.publish(new ItemDeliveredEvent("not the sigil", "shrine", player));
        bus.publish(new ItemDeliveredEvent("sigil", "wrong shrine", player));
        flush(bus);

        assertEquals(TaskStatus.ACTIVE, findItem.getStatus(), "wrong item must not complete FindTask");
        assertEquals(TaskStatus.ACTIVE, findNpc.getStatus(), "wrong NPC must not complete FindTask");
        assertEquals(TaskStatus.ACTIVE, deliver.getStatus(), "wrong item/destination must not complete DeliverTask");
        assertEquals(QuestState.ACTIVE, quest.getState(), "quest stays active");
    }

    private static void testQuestValidationAndDoubleStart() {
        expectIllegalArgument(() -> new Quest("empty", "Empty", List.of()), "a quest without segments must be rejected");
        expectIllegalArgument(() -> new Quest("hollow", "Hollow", List.of(new StorySegment("No tasks", "Never advances"))),
                "a segment without tasks must be rejected");

        EventBus bus = newStartedBus();
        JournalService journal = new JournalService(bus);
        QuestService quests = new QuestService(bus, journal);
        Quest quest = new Quest("q4", "Quest Four", List.of(
                new StorySegment("Only", "One task").addRequiredTask(new FindTask("f", "find it", "thing"))));

        quests.startQuest(quest);
        quests.startQuest(quest);
        assertEquals(1, quests.getActiveQuests().size(), "starting the same quest twice must not register it twice");
        waitUntil(() -> journal.getAllEntries().size() == 2, "start entries missing");
        flush(bus);
        assertEquals(2, journal.getAllEntries().size(), "second start must not write more journal entries");
    }

    // ------------------------------------------------------------- helpers

    private static EventBus newStartedBus() {
        EventBus bus = new EventBus();
        bus.initialize(null, null);
        return bus;
    }

    /** The dispatcher is a single FIFO thread, so once this marker is delivered everything published before it has been handled. */
    private static void flush(EventBus bus) {
        boolean[] delivered = new boolean[1];
        GameEventListener<FlushMarker> listener = e -> delivered[0] = true;
        bus.subscribe(FlushMarker.class, listener);
        bus.publish(new FlushMarker());
        waitUntil(() -> delivered[0], "event bus did not drain");
        bus.unsubscribe(FlushMarker.class, listener);
    }

    /** Test-only event: nothing under test reacts to it, so it can safely mark "everything before me has been handled". */
    private static final class FlushMarker implements GameEvent {
    }

    private static void waitUntil(BooleanSupplier condition, String failureMessage) {
        long deadline = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("interrupted while waiting: " + failureMessage, e);
            }
        }
        throw new AssertionError(failureMessage + " (timed out)");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + " (expected <" + expected + ">, was <" + actual + ">)");
        }
    }

    private static void expectIllegalArgument(Runnable action, String message) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError(message);
    }

    /** Only identity matters to the code under test, so a bare proxy is enough. */
    private static CharacterInterface fakeCharacter(String name) {
        return (CharacterInterface) Proxy.newProxyInstance(
                CharacterInterface.class.getClassLoader(),
                new Class<?>[]{CharacterInterface.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        case "equals":
                            return proxy == args[0];
                        case "toString":
                            return name;
                        default:
                            Class<?> type = method.getReturnType();
                            if (type == boolean.class) return false;
                            if (type == int.class) return 0;
                            if (type == float.class) return 0f;
                            return null;
                    }
                });
    }

}
