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

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Centralized asynchronous event bus.
 *
 * <p>Any thread may {@link #publish(GameEvent)}. Events are queued and
 * delivered by a single daemon dispatcher thread in FIFO order. Listeners
 * therefore always run on that thread, serialized, never on the publisher's
 * thread and never on the render thread.</p>
 *
 * <p>Dispatch walks the event's class hierarchy and implemented interfaces,
 * so subscribing to a supertype or an interface receives all matching
 * subtypes. A failing listener is logged and does not prevent other
 * listeners from receiving the event.</p>
 *
 * <p>This is a {@link BaseAppState} only so that {@link #initialize(Application)}
 * gives it a reliable place to start the dispatcher thread; it does zero
 * work on the render thread ({@code update(float)} is intentionally not
 * overridden). The dispatcher is a daemon thread with no shutdown hook: it
 * terminates with the JVM.</p>
 */
public final class EventBus extends BaseAppState {

    private static final Logger LOG = Logger.getLogger(EventBus.class.getName());

    private final Map<Class<?>, CopyOnWriteArrayList<GameEventListener<?>>> listeners
            = new ConcurrentHashMap<>();
    private final BlockingQueue<GameEvent> pending = new LinkedBlockingQueue<>();

    // ------------------------------------------------------------- API

    /**
     * Register {@code listener} for events of exactly the given {@code type},
     * or any of its subtypes (subscribing to an interface or an abstract
     * base is a valid, supported way to receive every event that implements
     * or extends it, including {@link GameEvent} itself for "every event").
     */
    public <T extends GameEvent> void subscribe(Class<T> type, GameEventListener<T> listener) {
        if (type == null || listener == null) {
            throw new IllegalArgumentException("type and listener must be non-null");
        }
        listeners.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /**
     * Remove a previously registered listener. A listener that unsubscribes
     * itself during its own dispatch may still receive the event currently
     * being delivered, since dispatch iterates a {@code CopyOnWriteArrayList}
     * snapshot; this is documented behavior, not a bug.
     */
    public <T extends GameEvent> void unsubscribe(Class<T> type, GameEventListener<T> listener) {
        if (type == null || listener == null) return;
        CopyOnWriteArrayList<GameEventListener<?>> list = listeners.get(type);
        if (list != null) {
            list.remove(listener);
        }
    }

    /**
     * Enqueue an event for asynchronous delivery. Safe to call from any
     * thread; never blocks (the queue is unbounded).
     */
    public void publish(GameEvent event) {
        if (event == null) return;
        pending.offer(event);
    }

    // --------------------------------------------------- AppState lifecycle

    @Override
    protected void initialize(Application app) {
        Thread dispatcher = new Thread(this::runLoop, "EventBus-Dispatcher");
        dispatcher.setDaemon(true);
        dispatcher.start();
    }

    @Override
    protected void cleanup(Application app) {
        // Intentionally empty. The dispatcher is a daemon thread and is
        // allowed to run until JVM exit; see the class-level javadoc.
    }

    @Override
    protected void onEnable() {
        // No-op: the bus does no per-frame work, so enable/disable has
        // nothing to toggle.
    }

    @Override
    protected void onDisable() {
        // No-op: see onEnable().
    }

    // ----------------------------------------------------------- internals

    private void runLoop() {
        while (true) {
            try {
                dispatch(pending.take());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Throwable t) {
                LOG.log(Level.SEVERE, "Dispatcher failure", t);
            }
        }
    }

    private void dispatch(GameEvent event) {
        Set<Class<?>> types = new LinkedHashSet<>();
        collectTypes(event.getClass(), types);
        for (Class<?> type : types) {
            CopyOnWriteArrayList<GameEventListener<?>> list = listeners.get(type);
            if (list == null) continue;
            for (GameEventListener<?> listener : list) {
                invokeUnchecked(listener, event);
            }
        }
    }

    private static void collectTypes(Class<?> type, Set<Class<?>> out) {
        if (type == null || !out.add(type)) return;
        collectTypes(type.getSuperclass(), out);
        for (Class<?> iface : type.getInterfaces()) {
            collectTypes(iface, out);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void invokeUnchecked(GameEventListener listener, GameEvent event) {
        try {
            listener.onEvent(event);
        } catch (Throwable t) {
            LOG.log(Level.SEVERE,
                    "Listener failed for " + event.getClass().getSimpleName(), t);
        }
    }
}
