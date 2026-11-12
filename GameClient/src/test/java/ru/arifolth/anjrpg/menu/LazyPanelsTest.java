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

import com.simsilica.lemur.Container;

/**
 * Manual smoke test for {@link LazyPanels}, in the same style as {@code EventBusTest}: no JUnit dependency
 * exists in this project, so this is a plain class with a {@code main()}; every check is a no-arg method that
 * throws {@link AssertionError} on failure.
 *
 * <p>The panels are fakes whose {@code getContainer()} is never called, so no Lemur UI has to be set up.</p>
 *
 * <p>Run from an IDE (run {@code main}), or from the command line:
 * {@code mvn -pl GameClient test-compile exec:java
 * -Dexec.classpathScope=test -Dexec.mainClass=ru.arifolth.anjrpg.menu.LazyPanelsTest}</p>
 */
public class LazyPanelsTest {

    private static int built = 0;

    private static final class FakePanel implements MenuPanel {
        FakePanel() {
            built++;
        }

        @Override
        public Container getContainer() {
            return null;
        }
    }

    public static void main(String[] args) {
        Runnable[] checks = {
                LazyPanelsTest::testRegisteringBuildsNothing,
                LazyPanelsTest::testBuiltOnFirstRequestOnlyAndOnlyOnce,
                LazyPanelsTest::testUnknownNameIsNull,
                LazyPanelsTest::testClearGivesFreshPanelsNextTime,
                LazyPanelsTest::testOpeningTheMenuBuildsOnePanelNotAll
        };
        String[] names = {
                "registering factories builds nothing",
                "a panel is built on first request only, and only once",
                "an unknown name gives null, not an exception",
                "clear() and re-registering gives fresh panels (what InGameMenuState does on every enable)",
                "ESC model: showing the first panel builds one panel, not all six"
        };

        int passed = 0;
        for (int i = 0; i < checks.length; i++) {
            try {
                built = 0;
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

    private static void testRegisteringBuildsNothing() {
        LazyPanels panels = new LazyPanels();
        panels.register("A", FakePanel::new);
        panels.register("B", FakePanel::new);
        assertEquals(0, built, "registering must not build anything");
    }

    private static void testBuiltOnFirstRequestOnlyAndOnlyOnce() {
        LazyPanels panels = new LazyPanels();
        panels.register("A", FakePanel::new);
        panels.register("B", FakePanel::new);

        MenuPanel first = panels.get("A");
        assertEquals(1, built, "only the requested panel may be built");
        assertTrue(first == panels.get("A"), "a second request must return the same panel");
        assertEquals(1, built, "a second request must not build again");
    }

    private static void testUnknownNameIsNull() {
        LazyPanels panels = new LazyPanels();
        panels.register("A", FakePanel::new);
        assertTrue(panels.get("nope") == null, "unknown name");
        assertEquals(0, built, "an unknown name builds nothing");
    }

    private static void testClearGivesFreshPanelsNextTime() {
        LazyPanels panels = new LazyPanels();
        panels.register("A", FakePanel::new);
        MenuPanel before = panels.get("A");

        panels.clear();
        assertTrue(panels.get("A") == null, "clear() forgets the factories too");

        panels.register("A", FakePanel::new);
        MenuPanel after = panels.get("A");
        assertTrue(before != after, "a re-registered panel is a new instance");
        assertEquals(2, built, "exactly two panels were ever built");
    }

    private static void testOpeningTheMenuBuildsOnePanelNotAll() {
        LazyPanels panels = new LazyPanels();
        for (String name : new String[]{"Equipment", "Inventory", "Status", "Journal", "System", "Exit"}) {
            panels.register(name, FakePanel::new);
        }

        panels.get("Equipment"); // what showPanel("Equipment") does at the end of onEnable()
        assertEquals(1, built, "opening the menu must build only the first panel");

        panels.get("Journal");
        assertEquals(2, built, "clicking another tab builds that one panel");
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
}
