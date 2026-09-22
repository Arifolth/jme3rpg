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

/**
 * Marker interface for every event that travels on the {@link EventBus}.
 *
 * <p>Concrete events are plain data carriers. Producers must not perform
 * rendering, physics, or asset loading inside an event. Event classes must
 * be immutable; object references carried by an event are to be treated as
 * read-only by consumers.</p>
 *
 * <p>An abstract base may be introduced later to group related events; the
 * bus dispatches by class hierarchy and interfaces, so subscribing to a base
 * receives all of its subtypes.</p>
 */
public interface GameEvent {
}
