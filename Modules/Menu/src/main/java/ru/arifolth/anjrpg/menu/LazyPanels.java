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

package ru.arifolth.anjrpg.menu;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Menu panels that are built the first time they are asked for, not all up front.
 *
 * <p>{@code InGameMenuState} and {@code SystemPanel} used to construct every panel they own the moment
 * they were enabled, although the player looks at one at a time. Pressing ESC built the Video and the
 * Controls panels - thirteen Lemur dropdowns, a reflective walk over {@code KeyInput} for each key
 * binding, and an OS display-mode query - none of which anyone was looking at. A registered factory costs
 * nothing until its panel is shown.</p>
 *
 * <p>The owner still gets fresh panels every time it is enabled: it {@link #clear()}s this registry and
 * registers its factories again, which drops everything built before. Like all Lemur UI code this is
 * meant for the render thread only and is not synchronized.</p>
 */
public final class LazyPanels {
    private final Map<String, Supplier<? extends MenuPanel>> factories = new HashMap<>();
    private final Map<String, MenuPanel> built = new HashMap<>();

    /** Registers (or replaces) the factory for {@code name}; nothing is built yet. */
    public void register(String name, Supplier<? extends MenuPanel> factory) {
        factories.put(name, factory);
        built.remove(name);
    }

    /**
     * @return the panel registered under {@code name}, built now if this is the first request since it
     * was registered, or {@code null} for a name nothing was registered under
     */
    public MenuPanel get(String name) {
        MenuPanel panel = built.get(name);
        if (panel == null) {
            Supplier<? extends MenuPanel> factory = factories.get(name);
            if (factory == null) {
                return null;
            }
            panel = factory.get();
            built.put(name, panel);
        }
        return panel;
    }

    /** Forgets every registered factory and every panel built from them. */
    public void clear() {
        factories.clear();
        built.clear();
    }
}
