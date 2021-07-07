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

package ru.arifolth.anjrpg;

import com.jme3.math.Vector3f;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.system.AppSettings;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.style.BaseStyles;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.Controller;
import de.lessvoid.nifty.controls.Parameters;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.input.NiftyInputEvent;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;

import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;

import static com.jme3.niftygui.NiftyJmeDisplay.newNiftyJmeDisplay;

/*
* https://ev1lbl0w.github.io/jme-wiki-pt-pt/jme3/advanced/loading_screen.html
* */
public class ANJRpg extends RolePlayingGame implements ScreenController, Controller {

    private NiftyJmeDisplay niftyDisplay;
    private Nifty nifty;
    private Boolean initialization = true;
    final private static Logger LOGGER = Logger.getLogger(ANJRpg.class.getName());
    private boolean ready = false;

    public static void main(String[] args) {
        app = new ANJRpg();
        app.start();
    }

    public ANJRpg() {
        initializeApplicationSettings();
    }

    @Override
    public void simpleInitApp()  {
        super.simpleInitApp();

        /* Lemur stuff */
        GuiGlobals.initialize(this);
        GuiGlobals globals = GuiGlobals.getInstance();
        BaseStyles.loadGlassStyle();
        globals.getStyles().setDefaultStyle("glass");

        setupPhysix();
        setupSound();
        setupGameLogic();
        setupTerrain();
    }

    @Override
    public void simpleUpdate(float tpf) {
        if(!ready) {
            return;
        }

        if(null == initialization) {
            super.simpleUpdate(tpf);
        } else if (initialization) {
            Element element = nifty.getScreen("loadlevel").findElementById("loadingtext");
            textRenderer = element.getRenderer(TextRenderer.class);

            loadResources();

            setProgress("Loading complete");
            initialization = false;
        } else if (initialization == false) {
            //wait until land appears in Physics Space
            if(bulletAppState.getPhysicsSpace().getRigidBodyList().size() == 4) {
                //put player at the beginning location
                getGameLogicCore().getPlayerCharacter().getCharacterControl().setPhysicsLocation(new Vector3f(0, -15, 0));

                //these calls have to be done on the update loop thread,
                //especially attaching the terrain to the rootNode
                //after it is attached, it's managed by the update loop thread
                // and may not be modified from any other thread anymore!
                nifty.gotoScreen("end");
                nifty.exit();
                guiViewPort.removeProcessor(niftyDisplay);
                initialization = null;

                createMinimap();
            }
        }
    }

    public void showLoadingMenu() {
        nifty.gotoScreen("loadlevel");
        initialization = true;
    }

    @Override
    public void onStartScreen() {
    }

    @Override
    public void onEndScreen() {
    }

    @Override
    public void bind(Nifty nifty, Screen screen) {
        progressBarElement = nifty.getScreen("loadlevel").findElementById("progressbar");
    }

    // methods for Controller
    @Override
    public boolean inputEvent(final NiftyInputEvent inputEvent) {
        return false;
    }

    @Override
    public void onFocus(boolean getFocus) {
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    @Override
    public void bind(Nifty nifty, Screen screen, Element elmnt,
                     Parameters prmtrs) {
        progressBarElement = elmnt.findElementById("progressbar");
    }

    @Override
    public void init(Parameters prmtrs) {
    }

    public void setUpGUI() {
        /* Game stuff */
        niftyDisplay = newNiftyJmeDisplay(assetManager,
                inputManager,
                audioRenderer,
                guiViewPort);
        nifty = niftyDisplay.getNifty();
        nifty.registerScreenController(this);
        nifty.fromXml("Interface/nifty_loading.xml", "loadlevel", this);

        guiViewPort.addProcessor(niftyDisplay);

        showLoadingMenu();
        ready = true;
    }

    private void initializeApplicationSettings() {
        if(ready) {
            return;
        }

        showSettings = false;

        AppSettings settings = new AppSettings(false);
        try {
            AppSettings oldSettings = new AppSettings(false);
            oldSettings.load("ru.arifolth.anjrpg");
            if(oldSettings.size() == 0) {
                oldSettings.copyFrom(new AppSettings(true));
                applyDefaultSettings(settings);
            }
            settings.mergeFrom(oldSettings);
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }

        //setDisplayFps(true);
        //setDisplayStatView(false);

        this.setSettings(settings);
        this.setShowSettings(showSettings);

        //do not output excessive info on console
        Logger.getLogger("").setLevel(Level.SEVERE);

        // hide FPS HUD
        setDisplayFps(false);

        //hide statistics HUD
        setDisplayStatView(false);
    }

    private void applyDefaultSettings(AppSettings settings) {
        settings.setTitle("Alexander's Nilov Java RPG");
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        settings.setFullscreen(device.isFullScreenSupported());
        settings.setBitsPerPixel(32); //24
        settings.setSamples(1); //16
        settings.setVSync(true);
        settings.setResolution(3840,2160);
        settings.setRenderer(AppSettings.LWJGL_OPENGL45);
        settings.setFrameRate(60);
        settings.setGammaCorrection(false);
    }

    public AppSettings getSettings(){
        return this.settings;
    }
}
