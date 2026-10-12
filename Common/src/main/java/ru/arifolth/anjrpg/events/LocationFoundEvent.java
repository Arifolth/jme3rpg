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

import ru.arifolth.anjrpg.interfaces.CharacterInterface;

/**
 * The player reached a point of interest that an active quest task is
 * looking for. Published by {@code LocationTracker} on a proximity check.
 * {@code poiId} is {@code POIInterface.getId()}.
 */
public final class LocationFoundEvent implements GameEvent {

    private final String poiId;
    private final CharacterInterface finder;

    public LocationFoundEvent(String poiId, CharacterInterface finder) {
        this.poiId = poiId;
        this.finder = finder;
    }

    public String getPoiId() {
        return poiId;
    }

    public CharacterInterface getFinder() {
        return finder;
    }

    @Override
    public String toString() {
        return "LocationFoundEvent{" + poiId + "}";
    }
}
