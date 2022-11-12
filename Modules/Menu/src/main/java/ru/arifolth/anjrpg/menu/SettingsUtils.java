/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2022 Alexander Nilov
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
import com.jme3.system.JmeSystem;
import com.jme3.system.JmeVersion;

import java.awt.*;
import java.util.prefs.BackingStoreException;

public class SettingsUtils {
    private SettingsUtils() {
    }

    public static void applyDefaultSettings(AppSettings settings) {
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        settings.setFullscreen(device.isFullScreenSupported());
        settings.setBitsPerPixel(24); //24
        settings.setSamples(0); //16
        settings.setVSync(false);
        settings.setResolution(1920,1080);
        settings.setRenderer(AppSettings.LWJGL_OPENGL45);
        settings.setFrameRate(30);
        settings.setFrequency(30);
        settings.setGammaCorrection(false);
        settings.setTitle(JmeVersion.FULL_NAME);
    }

    public static AppSettings loadSettings() {
        AppSettings settings = new AppSettings(false);

        try {
            settings.load(JmeVersion.FULL_NAME);
        } catch (BackingStoreException ex) {
            ex.printStackTrace();
        }

        if(settings.size() == 0) {
            settings = new AppSettings(true);
            applyDefaultSettings(settings);
        }

        //Native launcher BUG workaround - otherwise it will drop to some weird resolution
        settings.setSettingsDialogImage(null);
        if (!JmeSystem.showSettingsDialog(settings, true)) {
            return null;
        }
        settings.setRenderer(AppSettings.LWJGL_OPENGL45);
        settings.setFrameRate(30);

        return settings;
    }

    public static void saveSettings(AppSettings settings) {
        try {
            settings.save(JmeVersion.FULL_NAME);
        } catch (BackingStoreException ex) {
            ex.printStackTrace();
        }
    }
}
