/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2014 - 2024 Alexander Nilov
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
import ru.arifolth.anjrpg.interfaces.Constants;

import java.awt.*;
import java.util.prefs.BackingStoreException;

public class SettingsUtils {

    private SettingsUtils() {
    }

    public static void applyDefaultSettings(AppSettings settings) {
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        settings.setFullscreen(device.isFullScreenSupported());
        DisplayMode[] modes = device.getDisplayModes();
        DisplayMode mode = modes[modes.length-1];
        settings.setDepthBits(mode.getBitDepth());
        settings.setResolution(mode.getWidth(),mode.getHeight());
        settings.setRenderer(AppSettings.LWJGL_OPENGL45);
        settings.setFrequency(mode.getRefreshRate());
        settings.setGammaCorrection(false);
        settings.setStencilBits(Constants.STENCIL_BITS);
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
