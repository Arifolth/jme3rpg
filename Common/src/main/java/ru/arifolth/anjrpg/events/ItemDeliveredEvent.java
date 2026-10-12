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
 * A Quest Item was delivered to a point of interest. Pure event definition
 * for now: the future Items system's Quest item delivery function will
 * publish it. {@code poiId} is {@code POIInterface.getId()}.
 */
public final class ItemDeliveredEvent implements GameEvent {

    private final String itemId;
    private final String poiId;
    private final CharacterInterface deliverer;

    public ItemDeliveredEvent(String itemId, String poiId, CharacterInterface deliverer) {
        this.itemId = itemId;
        this.poiId = poiId;
        this.deliverer = deliverer;
    }

    public String getItemId() {
        return itemId;
    }

    public String getPoiId() {
        return poiId;
    }

    public CharacterInterface getDeliverer() {
        return deliverer;
    }

    @Override
    public String toString() {
        return "ItemDeliveredEvent{" + itemId + " -> " + poiId + "}";
    }
}
