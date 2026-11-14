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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * JUnit test for the cached {@link MenuUtils} key lookups.
 *
 * <p>{@link Legacy} is a copy of the reflective code {@code MenuUtils} used to run on every call. Each test
 * asserts that the cached version gives the same answer - or throws the same exception - for every constant
 * of the real {@code com.jme3.input.KeyInput}, including keys that share a value (both versions return the
 * first one declared).</p>
 */
public class MenuUtilsTest {

    @Test
    @DisplayName("getAllKeyboardKeys() equals the reflective version")
    void testAllKeyboardKeysMatchTheReflectiveVersion() {
        assertEquals(Legacy.getAllKeyboardKeys(), MenuUtils.getAllKeyboardKeys(), "key list");
    }

    @Test
    @DisplayName("getAllKeyboardKeys() returns an independent list each call")
    void testAllKeyboardKeysIsACopyEachTime() {
        List<String> first = MenuUtils.getAllKeyboardKeys();
        int size = first.size();
        first.clear();
        assertEquals(size, MenuUtils.getAllKeyboardKeys().size(), "mutating a returned list must not affect the next one");
    }

    @Test
    @DisplayName("getKey(name) equals the reflective version for every constant")
    void testKeyByNameMatches() {
        for (String name : Legacy.fieldNames()) {
            assertEquals(outcome(() -> Legacy.getKey(name)), outcome(() -> MenuUtils.getKey(name)), "getKey(" + name + ")");
        }
    }

    @Test
    @DisplayName("getKeyName(int) equals the reflective version for every value (first declared wins)")
    void testKeyNameByValueMatchesIncludingDuplicates() {
        for (String name : Legacy.fieldNames()) {
            int value = (int) outcomeValue(() -> Legacy.getKey(name));
            assertEquals(outcome(() -> Legacy.getKeyName(value)), outcome(() -> MenuUtils.getKeyName(value)), "getKeyName(" + value + ") for " + name);
        }
    }

    @Test
    @DisplayName("getKeyName(String suffix) equals the reflective version for every constant")
    void testKeyNameBySuffixMatches() {
        for (String name : Legacy.fieldNames()) {
            String suffix = name.substring(name.indexOf('_') + 1);
            assertEquals(outcome(() -> Legacy.getKeyName(suffix)), outcome(() -> MenuUtils.getKeyName(suffix)), "getKeyName(\"" + suffix + "\")");
        }
    }

    @Test
    @DisplayName("unknown names and values fail the same way")
    void testUnknownLookupsFailTheSameWay() {
        assertEquals(outcome(() -> Legacy.getKey("KEY_NOPE")), outcome(() -> MenuUtils.getKey("KEY_NOPE")), "unknown name");
        assertEquals(outcome(() -> Legacy.getKeyName(-12345)), outcome(() -> MenuUtils.getKeyName(-12345)), "unknown value");
        assertEquals(outcome(() -> Legacy.getKeyName("NOPE_NOPE")), outcome(() -> MenuUtils.getKeyName("NOPE_NOPE")), "unknown suffix");
    }

    // ------------------------------------------------------------- helpers

    private interface Lookup {
        Object run() throws Exception;
    }

    private static Object outcomeValue(Lookup lookup) {
        try {
            return lookup.run();
        } catch (Exception e) {
            throw new AssertionError("lookup unexpectedly failed: " + e);
        }
    }

    /** the value, or the exception's class and message - so "same result" includes "same failure" */
    private static String outcome(Lookup lookup) {
        try {
            return "value:" + lookup.run();
        } catch (Exception e) {
            return "threw:" + e.getClass().getName() + ":" + e.getMessage();
        }
    }

    /** The code MenuUtils ran on every call before it cached KeyInput - kept here as the reference. */
    private static final class Legacy {
        private static final Class<?> clazz;

        static {
            try {
                clazz = Class.forName("com.jme3.input.KeyInput");
            } catch (ClassNotFoundException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        static List<String> fieldNames() {
            List<String> names = new ArrayList<>();
            for (Field field : clazz.getDeclaredFields()) {
                names.add(field.getName());
            }
            return names;
        }

        static String getKeyName(String name) throws NoSuchFieldException {
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                if (field.getName().contains("_" + name)) {
                    return field.getName();
                }
            }
            throw new NoSuchFieldException(name);
        }

        static String getKeyName(int key) throws NoSuchFieldException, IllegalAccessException {
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                if (field.getInt(clazz) == key) {
                    return field.getName();
                }
            }
            throw new NoSuchFieldException(String.valueOf(key));
        }

        static int getKey(String name) throws NoSuchFieldException, IllegalAccessException {
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                if (field.getName().equals(name)) {
                    return field.getInt(clazz);
                }
            }
            throw new NoSuchFieldException(name);
        }

        static List<String> getAllKeyboardKeys() {
            List<String> keys = new ArrayList<>();
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                if (Modifier.isStatic(field.getModifiers())) {
                    keys.add(field.getName());
                }
            }
            keys.remove("KEY_ESCAPE");
            keys.remove("KEY_UNKNOWN");
            return keys;
        }
    }
}
