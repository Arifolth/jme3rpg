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
import com.simsilica.lemur.component.BorderLayout;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuUtils {
    // KeyInput's constants never change, so they are read once instead of walking its fields with reflection on
    // every call - the Controls panel alone made about two dozen such calls (one pass over every field of
    // KeyInput, with setAccessible on each) every time it was built. The lookups return the first match, so all
    // of these keep the order the fields are declared in.
    private static final List<String> FIELD_NAMES = new ArrayList<>();        // every declared field
    private static final List<String> STATIC_FIELD_NAMES = new ArrayList<>(); // the static ones
    private static final Map<String, Integer> NAME_TO_KEY = new HashMap<>();
    private static final Map<Integer, String> KEY_TO_NAME = new HashMap<>();

    static {
        try {
            Class<?> clazz = Class.forName("com.jme3.input.KeyInput");
            for (Field field : clazz.getDeclaredFields()) {
                FIELD_NAMES.add(field.getName());
                if (Modifier.isStatic(field.getModifiers())) {
                    STATIC_FIELD_NAMES.add(field.getName());
                }
                if (field.getType() == int.class) {
                    field.setAccessible(true);
                    int value = field.getInt(clazz);
                    NAME_TO_KEY.putIfAbsent(field.getName(), value);
                    KEY_TO_NAME.putIfAbsent(value, field.getName());
                }
            }
        } catch (ClassNotFoundException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private MenuUtils() {}

    public static String getKeyName(String name) throws NoSuchFieldException {
        for (String fieldName : FIELD_NAMES) {
            if (fieldName.contains("_" + name)) {
                return fieldName;
            }
        }
        throw new NoSuchFieldException(name);
    }

    public static String getKeyName(int key) throws NoSuchFieldException, IllegalAccessException {
        String name = KEY_TO_NAME.get(key);
        if (name == null) {
            throw new NoSuchFieldException(String.valueOf(key));
        }
        return name;
    }

    public static int getKey(String name) throws NoSuchFieldException, IllegalAccessException {
        Integer key = NAME_TO_KEY.get(name);
        if (key == null) {
            throw new NoSuchFieldException(name);
        }
        return key;
    }

    public static List<String> getAllKeyboardKeys() {
        List<String> keys = new ArrayList<>(STATIC_FIELD_NAMES);

        keys.remove("KEY_ESCAPE");
        keys.remove("KEY_UNKNOWN");

        return keys;
    }

    static Container createContainerWithBorderLayout() {
        return new Container(new BorderLayout());
    }
}
