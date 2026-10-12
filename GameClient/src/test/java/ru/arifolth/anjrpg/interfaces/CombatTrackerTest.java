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

package ru.arifolth.anjrpg.interfaces;

import com.jme3.math.Vector3f;
import ru.arifolth.anjrpg.events.CombatDisengagedEvent;
import ru.arifolth.anjrpg.events.CombatEngagedEvent;
import ru.arifolth.anjrpg.events.EventBus;
import ru.arifolth.anjrpg.events.GameEvent;
import ru.arifolth.anjrpg.events.GameEventListener;
import ru.arifolth.anjrpg.events.GameStateChangedEvent;
import ru.arifolth.anjrpg.events.NpcKilledEvent;
import ru.arifolth.anjrpg.events.PlayerDiedEvent;

import java.lang.reflect.Proxy;
import java.util.function.BooleanSupplier;

/**
 * Manual smoke test for the event-driven {@link CombatTracker}, in the same
 * style as {@code EventBusTest}: no JUnit dependency exists in this project,
 * so this is a plain class with a {@code main()}; every check is a no-arg
 * method that throws {@link AssertionError} on failure.
 *
 * <p>Runs against a real {@link EventBus} (started the same way
 * {@code EventBusTest} does). The only fakes are a {@link CharacterInterface}
 * proxy (identity is all the tracker uses) and a minimal
 * {@link GameStateManagerInterface} that applies proposals immediately through
 * the real {@code GameState#isNextAcceptable} rules.</p>
 *
 * <p>Run from an IDE (run {@code main}), or from the command line:
 * {@code mvn -pl GameClient test-compile exec:java
 * -Dexec.classpathScope=test -Dexec.mainClass=ru.arifolth.anjrpg.interfaces.CombatTrackerTest}</p>
 */
public class CombatTrackerTest {

    public static void main(String[] args) {
        Runnable[] checks = {
                CombatTrackerTest::testCombatEngageDisengageAndKill,
                CombatTrackerTest::testCombatPlayerDeathAndSelfHealing
        };
        String[] names = {
                "combat tracker: engage / disengage / kill drive BATTLE and CALM",
                "combat tracker: player death clears it; state-change events self-heal it"
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

    // ------------------------------------------------------------- combat

    private static void testCombatEngageDisengageAndKill() {
        EventBus bus = newStartedBus();
        FakeGameStateManager states = new FakeGameStateManager(GameState.CALM);
        CombatTracker tracker = new CombatTracker(states, bus);
        CharacterInterface a = fakeCharacter("A");
        CharacterInterface b = fakeCharacter("B");

        bus.publish(new CombatEngagedEvent(a));
        waitUntil(() -> GameState.BATTLE.equals(states.current), "first engagement did not start BATTLE");
        assertTrue(tracker.isInCombat(), "isInCombat should be true while engaged");

        bus.publish(new CombatEngagedEvent(b));
        bus.publish(new CombatDisengagedEvent(a));
        flush(bus);
        assertEquals(GameState.BATTLE, states.current, "still engaged with B, must stay in BATTLE");
        assertTrue(tracker.isInCombat(), "still engaged with B");

        bus.publish(new NpcKilledEvent(b, new Vector3f(1, 2, 3)));
        waitUntil(() -> GameState.CALM.equals(states.current), "killing the last engaged NPC did not end BATTLE");
        assertTrue(!tracker.isInCombat(), "isInCombat should be false once nobody is engaged");
    }

    private static void testCombatPlayerDeathAndSelfHealing() {
        EventBus bus = newStartedBus();
        FakeGameStateManager states = new FakeGameStateManager(GameState.CALM);
        CombatTracker tracker = new CombatTracker(states, bus);
        CharacterInterface a = fakeCharacter("A");

        // a pending CALM committing while still engaged must be corrected by the next state-change event
        bus.publish(new CombatEngagedEvent(a));
        waitUntil(() -> GameState.BATTLE.equals(states.current), "engagement did not start BATTLE");
        states.current = GameState.CALM;
        bus.publish(new GameStateChangedEvent(GameState.BATTLE, GameState.CALM));
        waitUntil(() -> GameState.BATTLE.equals(states.current), "tracker did not re-propose BATTLE while still engaged");

        // player dies: tracker resets and stops proposing anything
        bus.publish(new PlayerDiedEvent(fakeCharacter("player")));
        waitUntil(() -> !tracker.isInCombat(), "player death did not clear combat");
        states.current = GameState.DEATH;
        bus.publish(new CombatEngagedEvent(a));
        flush(bus);
        assertEquals(GameState.DEATH, states.current, "no state may be proposed while the player is dead");

        // respawn (DEATH -> CALM) re-arms the tracker
        states.current = GameState.CALM;
        bus.publish(new GameStateChangedEvent(GameState.DEATH, GameState.CALM));
        flush(bus);
        assertEquals(GameState.CALM, states.current, "an engagement reported while dead must not leak into the respawned game");
        bus.publish(new CombatEngagedEvent(a));
        waitUntil(() -> GameState.BATTLE.equals(states.current), "tracker did not resume after respawn");
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

    /** Applies proposals immediately, using the real transition rules. */
    private static final class FakeGameStateManager implements GameStateManagerInterface {
        volatile GameState current;

        FakeGameStateManager(GameState initial) {
            this.current = initial;
        }

        @Override
        public void setGameState(GameState gameState) {
            if (current.isNextAcceptable(gameState)) {
                current = gameState;
            }
        }

        @Override
        public GameState getCurrentGameState() {
            return current;
        }

        @Override
        public void update(float tpf) {
        }

        @Override
        public void changeState(float tpf) {
        }

        @Override
        public void checkTime() {
        }
    }
}
