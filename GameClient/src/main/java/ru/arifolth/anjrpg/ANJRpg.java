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

import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.system.AppSettings;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.style.BaseStyles;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.Parameters;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.input.NiftyInputEvent;
import de.lessvoid.nifty.screen.Screen;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import ru.arifolth.game.InitStateEnum;
import ru.arifolth.game.RolePlayingGameInterface;

import java.awt.*;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;

import static com.jme3.niftygui.NiftyJmeDisplay.newNiftyJmeDisplay;

/*
* https://ev1lbl0w.github.io/jme-wiki-pt-pt/jme3/advanced/loading_screen.html
* */
public class ANJRpg extends RolePlayingGame implements ANJRpgInterface {
    public static final int RIGID_BODIES_SIZE = 4;
    private NiftyJmeDisplay niftyDisplay;
    private Nifty nifty;
    private InitStateEnum initialization = InitStateEnum.PENDING;
    final private static Logger LOGGER = Logger.getLogger(ANJRpg.class.getName());
    private boolean loadingCompleted = false;

    private static RolePlayingGameInterface app;

    public static void main(String[] args) throws XmlPullParserException, IOException {
        app = new ANJRpg();
        app.start();
    }

    public ANJRpg() throws XmlPullParserException, IOException {
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
        setupTerrain();
        setupGameLogic();
    }

    @Override
    public void simpleUpdate(float tpf) {
        switch (initialization) {
            case PENDING: {
                if(!loadingCompleted) {
                    return;
                }

                Element element = nifty.getScreen("loadlevel").findElementById("loadingtext");
                textRenderer = element.getRenderer(TextRenderer.class);

                loadResources();

                setProgress("Loading complete");
                initialization = InitStateEnum.INITIALIZED;
                break;
            }
            case INITIALIZED: {
                //wait until land appears in Physics Space
                if (bulletAppState.getPhysicsSpace().getRigidBodyList().size() == RIGID_BODIES_SIZE) {
                    //put player at the beginning location
                    getGameLogicCore().positionPlayer();
                    //position NPCs around the Player
                    getGameLogicCore().positionNPCs(getGameLogicCore().getCharacterMap());

                    //these calls have to be done on the update loop thread,
                    //especially attaching the terrain to the rootNode
                    //after it is attached, it's managed by the update loop thread
                    // and may not be modified from any other thread anymore!
                    nifty.gotoScreen("end");
                    nifty.exit();
                    guiViewPort.removeProcessor(niftyDisplay);
                    initialization = InitStateEnum.RUNNING;

                    createMinimap();
                }
                break;
            }
            case RUNNING: {
                //run game
                super.simpleUpdate(tpf);
            }
        }
    }

    public void showLoadingMenu() {
        nifty.gotoScreen("loadlevel");
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

    @Override
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
        loadingCompleted = true;
    }

    private void initializeApplicationSettings() {
        if(loadingCompleted) {
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
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        settings.setFullscreen(device.isFullScreenSupported());
        settings.setBitsPerPixel(24); //24
        settings.setSamples(0); //16
        settings.setVSync(true);
        settings.setResolution(1280,1024);
        settings.setRenderer(AppSettings.LWJGL_OPENGL45);
        settings.setFrameRate(30);
        settings.setGammaCorrection(false);
    }

    @Override
    public InitStateEnum getInitStatus() {
        return initialization;
    }

    @Override
    public AppSettings getSettings(){
        return this.settings;
    }
}
