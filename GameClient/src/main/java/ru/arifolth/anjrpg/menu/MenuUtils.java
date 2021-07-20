/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2021 Alexander Nilov
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

import com.jme3.system.AppSettings;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;

public class MenuUtils {
    //HKEY_CURRENT_USER\Software\JavaSoft\Prefs\ru\arifolth\anjrpg
    public static final String PREFERENCES_KEY = "ru.arifolth.anjrpg";
    private static Class clazz;

    static {
        try {
            clazz = Class.forName("com.jme3.input.KeyInput");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private MenuUtils() {}

    public static String getKeyName(String name) throws NoSuchFieldException {
        Field[] declaredFields = clazz.getDeclaredFields();
        for (Field field : declaredFields) {
            field.setAccessible(true);
            if( field.getName().contains("_" + name)){
                return field.getName();
            }
        }
        throw new NoSuchFieldException(name);
    }

    public static String getKeyName(int key) throws NoSuchFieldException, IllegalAccessException {
        Field[] declaredFields = clazz.getDeclaredFields();
        for (Field field : declaredFields) {
            field.setAccessible(true);
            if( field.getInt(clazz) == key){
                return field.getName();
            }
        }
        throw new NoSuchFieldException(String.valueOf(key));
    }

    public static int getKey(String name) throws NoSuchFieldException, IllegalAccessException {
        Field[] declaredFields = clazz.getDeclaredFields();
        for (Field field : declaredFields) {
            field.setAccessible(true);
            if( field.getName().equals(name)){
                return field.getInt(clazz);
            }
        }
        throw new NoSuchFieldException(name);
    }

    public static List<String> getAllKeyboardKeys() {
        List<String> keys = new ArrayList<>();

        Field[] declaredFields = clazz.getDeclaredFields();
        for (Field field : declaredFields) {
            field.setAccessible(true);
            if( Modifier.isStatic(field.getModifiers())){
//                System.out.println(field.getName());
                keys.add(field.getName());
            }
        }

        keys.remove("KEY_ESCAPE");
        keys.remove("KEY_UNKNOWN");

        return keys;
    }

    public static void saveSettings(AppSettings settings) {
        try {
            settings.save(PREFERENCES_KEY);
        } catch (BackingStoreException ex) {
            ex.printStackTrace();
        }
    }
}
