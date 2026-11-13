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

package ru.arifolth.anjrpg;

import com.jme3.app.FlyCamAppState;
import com.jme3.app.StatsAppState;
import com.jme3.cursors.plugins.JmeCursor;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.texture.Texture;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.style.BaseStyles;
import ru.arifolth.anjrpg.interfaces.*;
import ru.arifolth.anjrpg.loading.LoadingState;
import ru.arifolth.anjrpg.menu.SettingsUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class ANJRpg extends RolePlayingGame implements ANJRpgInterface {
    private InitStateEnum initialization = InitStateEnum.PENDING;
    final private static Logger LOGGER = Logger.getLogger(ANJRpg.class.getName());

    private static RolePlayingGameInterface app;

    static {
        Arrays.stream(LogManager.getLogManager().getLogger(Constants.ROOT_LOGGER).getHandlers()).forEach(h -> h.setLevel(Level.INFO));
    }
    public static void main(String[] args) throws IOException {
        app = new ANJRpg();
        app.setPauseOnLostFocus(false);
        app.start();
    }


    @Override
    public void start() {
        if (settings == null) {
            AppSettings loadedSettings = SettingsUtils.loadSettings();
            if(loadedSettings == null)
                return;
            setSettings(loadedSettings);
            SettingsUtils.saveSettings(settings);
        }
        start(JmeContext.Type.Display, true);
    }

    public ANJRpg() throws IOException {
        setShowSettings(showSettings);

        //do not output excessive info on console
        Logger.getLogger(Constants.ROOT_LOGGER).setLevel(Constants.LOGGING_LEVEL);

//        stateManager.attach(new StatsAppState());
//
//        // hide FPS HUD
//        setDisplayFps(true);
//
//        //hide statistics HUD
//        setDisplayStatView(true);
    }

    @Override
    public void simpleInitApp()  {
        super.simpleInitApp();

        stateManager.attach(new RenderingThreadPriorityAppState());

        stateManager.detach(stateManager.getState(FlyCamAppState.class));

        setupLemur();

        setupPhysix();
        setupSound();
        setupTerrain();
        setupGameLogic();
        initTerrain();
    }

    private void setupLemur() {
        /* Lemur stuff */
        GuiGlobals.initialize(this);
        GuiGlobals globals = GuiGlobals.getInstance();
        BaseStyles.loadGlassStyle();
        globals.getStyles().setDefaultStyle("glass");

        loadHardwareCursor();
    }

    private void loadHardwareCursor() {
        try {
            Texture cursorTexture = assetManager.loadTexture("Interface/Cursors/cursor.png");

            JmeCursor customCursor = MouseUtils.getJmeCursor(cursorTexture);

            inputManager.setMouseCursor(customCursor);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Cursor loading failed", e);
            // Fallback to system cursor
            inputManager.setMouseCursor(null);
        } finally {
            inputManager.setCursorVisible(false);
            GuiGlobals.getInstance().setCursorEventsEnabled(false);
        }
    }

    @Override
    public void simpleUpdate(float tpf) {
        switch (initialization) {
            case RUNNING:
                super.simpleUpdate(tpf);
                break;
            case PENDING:
                // Only update sound while the menu is showing and no loading has begun.
                if (stateManager.getState(LoadingState.class) == null) {
                    gameLogicCore.getSoundManager().update(tpf);
                }
                break;
            case INITIALIZED:
            case STOP:
            default:
                break;
        }
    }

    @Override
    public void setUpGUI() {
        initialization = InitStateEnum.INITIALIZED;
        stateManager.attach(new LoadingState());
    }

    @Override
    public void onLoadingComplete() {
        initialization = InitStateEnum.RUNNING;
    }

    @Override
    public InitStateEnum getInitStatus() {
        return initialization;
    }

    @Override
    public AppSettings getSettings(){
        return this.settings;
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    @Override
    public void stop() {
        initialization = InitStateEnum.STOP;
        super.stop(); // Triggers JME3 cleanup
        System.exit(0); // Force JVM termination after cleanup
    }
}
