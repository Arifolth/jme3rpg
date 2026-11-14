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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.arifolth.anjrpg.interfaces.GameState;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * JUnit test for {@link EventBus}, covering the six cases from the
 * EventBus specification, section 6.1.
 *
 * <p>Run it with {@code mvn -pl GameClient test}, or from an IDE.</p>
 */
public class EventBusTest {

    /**
     * Starts the dispatcher thread the same way the real jME AppState
     * lifecycle does: via {@code AppState.initialize(AppStateManager,
     * Application)}, which {@code BaseAppState} implements as {@code final}
     * and which calls our {@code initialize(Application)} override. Both
     * arguments are unused by {@link EventBus}, so {@code null} is safe
     * here without spinning up a real jME context.
     */
    private static EventBus newStartedBus() {
        EventBus bus = new EventBus();
        bus.initialize(null, null);
        return bus;
    }

    @Test
    @DisplayName("subscribe + publish delivers asynchronously, on the dispatcher thread")
    void testSubscribeAndPublishIsAsync() {
        EventBus bus = newStartedBus();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> threadName = new AtomicReference<>();

        GameEventListener<GameStateChangedEvent> listener = event -> {
            threadName.set(Thread.currentThread().getName());
            latch.countDown();
        };
        bus.subscribe(GameStateChangedEvent.class, listener);

        bus.publish(new GameStateChangedEvent(GameState.MENU, GameState.CALM));
        await(latch, "event was not delivered");
        assertEquals("EventBus-Dispatcher", threadName.get(), "listener did not run on the dispatcher thread");
    }

    @Test
    @DisplayName("interface dispatch (GameEvent.class catches every event)")
    void testInterfaceDispatch() {
        EventBus bus = newStartedBus();
        CountDownLatch latch = new CountDownLatch(1);

        bus.subscribe(GameEvent.class, (GameEventListener<GameEvent>) event -> latch.countDown());

        bus.publish(new GameStateChangedEvent(GameState.MENU, GameState.CALM));
        await(latch, "a GameEvent.class subscriber did not receive a GameStateChangedEvent");
    }

    // Test-only base + subclass for the superclass-dispatch check (§6.1.3).
    private static abstract class BaseTestEvent implements GameEvent {
    }

    private static final class LeafTestEvent extends BaseTestEvent {
    }

    @Test
    @DisplayName("superclass dispatch (abstract base catches its subtypes)")
    void testSuperclassDispatch() {
        EventBus bus = newStartedBus();
        CountDownLatch latch = new CountDownLatch(1);

        bus.subscribe(BaseTestEvent.class, (GameEventListener<BaseTestEvent>) event -> latch.countDown());

        bus.publish(new LeafTestEvent());
        await(latch, "a subscriber on the abstract base did not receive the leaf subtype");
    }

    @Test
    @DisplayName("unsubscribe stops delivery")
    void testUnsubscribeStopsDelivery() {
        EventBus bus = newStartedBus();
        AtomicBoolean delivered = new AtomicBoolean(false);

        GameEventListener<GameStateChangedEvent> listener = event -> delivered.set(true);
        bus.subscribe(GameStateChangedEvent.class, listener);
        bus.unsubscribe(GameStateChangedEvent.class, listener);

        bus.publish(new GameStateChangedEvent(GameState.MENU, GameState.CALM));

        // Fence: the dispatcher is single-threaded and FIFO, so once a
        // *later* published event has been delivered, the (unsubscribed)
        // one published just above has already been processed one way or
        // the other - no arbitrary sleep needed.
        CountDownLatch fence = new CountDownLatch(1);
        bus.subscribe(GameEvent.class, (GameEventListener<GameEvent>) event -> fence.countDown());
        bus.publish(new LeafTestEvent());
        await(fence, "fence event was not delivered");

        assertFalse(delivered.get(), "unsubscribed listener was still invoked");
    }

    @Test
    @DisplayName("listener failure is isolated")
    void testListenerFailureIsIsolated() {
        EventBus bus = newStartedBus();
        CountDownLatch secondRan = new CountDownLatch(1);

        GameEventListener<GameStateChangedEvent> throwing = event -> {
            throw new RuntimeException("boom (expected - testing isolation)");
        };
        GameEventListener<GameStateChangedEvent> second = event -> secondRan.countDown();

        bus.subscribe(GameStateChangedEvent.class, throwing);
        bus.subscribe(GameStateChangedEvent.class, second);

        bus.publish(new GameStateChangedEvent(GameState.MENU, GameState.CALM));
        await(secondRan, "second listener did not run after the first one threw");
    }

    @Test
    @DisplayName("publish from another thread is still delivered on the dispatcher thread")
    void testPublishFromAnotherThread() {
        EventBus bus = newStartedBus();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> threadName = new AtomicReference<>();

        bus.subscribe(GameStateChangedEvent.class, event -> {
            threadName.set(Thread.currentThread().getName());
            latch.countDown();
        });

        Thread publisher = new Thread(
                () -> bus.publish(new GameStateChangedEvent(GameState.MENU, GameState.CALM)),
                "test-publisher-thread");
        publisher.start();
        try {
            publisher.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("interrupted while joining the publisher thread", e);
        }

        await(latch, "event published from another thread was not delivered");
        assertEquals("EventBus-Dispatcher", threadName.get(), "listener did not run on the dispatcher thread");
    }

    private static void await(CountDownLatch latch, String failureMessage) {
        try {
            if (!latch.await(2, TimeUnit.SECONDS)) {
                throw new AssertionError(failureMessage + " (timed out)");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("interrupted while waiting: " + failureMessage, e);
        }
    }
}
