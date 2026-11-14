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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.simsilica.lemur.Container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit test for {@link LazyPanels}.
 *
 * <p>The panels are fakes whose {@code getContainer()} is never called, so no Lemur UI has to be set up.</p>
 */
public class LazyPanelsTest {

    private int built;

    private final class FakePanel implements MenuPanel {
        FakePanel() {
            built++;
        }

        @Override
        public Container getContainer() {
            return null;
        }
    }

    @Test
    @DisplayName("registering factories builds nothing")
    void testRegisteringBuildsNothing() {
        LazyPanels panels = new LazyPanels();
        panels.register("A", FakePanel::new);
        panels.register("B", FakePanel::new);
        assertEquals(0, built, "registering must not build anything");
    }

    @Test
    @DisplayName("a panel is built on first request only, and only once")
    void testBuiltOnFirstRequestOnlyAndOnlyOnce() {
        LazyPanels panels = new LazyPanels();
        panels.register("A", FakePanel::new);
        panels.register("B", FakePanel::new);

        MenuPanel first = panels.get("A");
        assertEquals(1, built, "only the requested panel may be built");
        assertSame(first, panels.get("A"), "a second request must return the same panel");
        assertEquals(1, built, "a second request must not build again");
    }

    @Test
    @DisplayName("an unknown name gives null, not an exception")
    void testUnknownNameIsNull() {
        LazyPanels panels = new LazyPanels();
        panels.register("A", FakePanel::new);
        assertNull(panels.get("nope"), "unknown name");
        assertEquals(0, built, "an unknown name builds nothing");
    }

    @Test
    @DisplayName("clear() and re-registering gives fresh panels (what InGameMenuState does on every enable)")
    void testClearGivesFreshPanelsNextTime() {
        LazyPanels panels = new LazyPanels();
        panels.register("A", FakePanel::new);
        MenuPanel before = panels.get("A");

        panels.clear();
        assertNull(panels.get("A"), "clear() forgets the factories too");

        panels.register("A", FakePanel::new);
        MenuPanel after = panels.get("A");
        assertTrue(before != after, "a re-registered panel is a new instance");
        assertEquals(2, built, "exactly two panels were ever built");
    }

    @Test
    @DisplayName("ESC model: showing the first panel builds one panel, not all six")
    void testOpeningTheMenuBuildsOnePanelNotAll() {
        LazyPanels panels = new LazyPanels();
        for (String name : new String[]{"Equipment", "Inventory", "Status", "Journal", "System", "Exit"}) {
            panels.register(name, FakePanel::new);
        }

        panels.get("Equipment"); // what showPanel("Equipment") does at the end of onEnable()
        assertEquals(1, built, "opening the menu must build only the first panel");

        panels.get("Journal");
        assertEquals(2, built, "clicking another tab builds that one panel");
    }
}
