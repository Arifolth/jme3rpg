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

package ru.arifolth.anjrpg;

import com.idflood.sky.DynamicSky;
import com.jayfella.minimap.MiniMapState;
import com.jme3.app.FlyCamAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.audio.AudioListenerState;
import com.jme3.bullet.BulletAppState;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import com.simsilica.lemur.OptionPanelState;
import com.simsilica.lemur.event.PopupState;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.tools.SizeValue;
import ru.arifolth.anjrpg.interfaces.*;
import ru.arifolth.anjrpg.menu.MainMenuState;
import ru.arifolth.sound.SoundManager;
import ru.arifolth.terrain.TerrainManager;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class RolePlayingGame extends SimpleApplication implements RolePlayingGameInterface {
    protected String version;
    protected Element progressBarElement;
    protected TextRenderer textRenderer;
    private volatile float progress;
    final private static Logger LOGGER = Logger.getLogger(RolePlayingGame.class.getName());
    protected SkyInterface sky;
    private TerrainManagerInterface terrainManager;
    private SoundManagerInterface soundManager;
    private FilterManagerInterface filterManager;
    protected BulletAppState bulletAppState;
    protected GameLogicCoreInterface gameLogicCore;

    public RolePlayingGame() {
        super(new FlyCamAppState(),
                new AudioListenerState(),
                new PopupState(),
                new OptionPanelState(),
                new MainMenuState()
        );

        try {
            PropertiesReader reader = new PropertiesReader(Constants.VERSION_PROPERTIES);
            version = reader.getProperty("version");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Unable to read version info!", e);
        }
    }

    @Override
    public void simpleInitApp() {
        /* Game stuff */
        setupAssetManager();
    }

    protected void loadResources() {
        setupScreenCapture();

        attachTerrain();

        initializeEntities();

        setupSky();

        setupFilters();
    }

    private void initializeEntities() {
        gameLogicCore.getInitializationDelegate().initialize(false);
    }


    protected void createMinimap() {
        // create the minimap
        MiniMapState miniMapState = new MiniMapState(getRootNode());
        stateManager.attach(miniMapState);
    }

    void setupGameLogic() {
        gameLogicCore = new GameLogicCore(this, cam, flyCam, inputManager, bulletAppState, assetManager, soundManager, terrainManager, getRootNode());
        gameLogicCore.initialize();
    }

    private void setupAssetManager() {
        assetManager.registerLocator("assets", ClasspathLocator.class);
    }

    /**
     * This is the main event loop--walking happens here.
     * We check in which direction the playerControl is walking by interpreting
     * the camera direction forward (camDir) and to the side (camLeft).
     * The setWalkDirection() command is what lets a physics-controlled playerControl walk.
     * We also make sure here that the camera moves with playerControl.
     */
    @Override
    public void simpleUpdate(float tpf) {
        gameLogicCore.update(tpf);

        terrainManager.update(tpf);

        sky.update(tpf);

        filterManager.update(tpf);

        soundManager.update(tpf);
    }

    void setupFilters() {
        filterManager = new FilterManager(assetManager, rootNode, viewPort, sky);
        filterManager.initialize();

        setProgress(new Object(){}.getClass().getEnclosingMethod().getName());
    }

    void setupSky() {
        // load sky
        sky = new DynamicSky(assetManager, viewPort, gameLogicCore);
        setProgress(new Object(){}.getClass().getEnclosingMethod().getName());
    }

    protected void attachTerrain() {
        enqueue(() -> {
            // execute in the jME3 rendering thread.
            getRootNode().attachChild(terrainManager.getTerrain());
        });
    }

    @Override
    public GameLogicCoreInterface getGameLogicCore() {
        return gameLogicCore;
    }

    @Override
    public synchronized Node getRootNode() {
        return this.rootNode;
    }

    void setupPhysix() {
        /** Set up Physics */
        bulletAppState = new BulletAppState();
        bulletAppState.setThreadingType(BulletAppState.ThreadingType.PARALLEL);
        stateManager.attach(bulletAppState);
        bulletAppState.setEnabled(true);
        //collision capsule shape is visible in debug mode
        AppSettings settings = this.getContext().getSettings();
        boolean debug = settings.getBoolean(Constants.DEBUG);
        if(debug) {
            bulletAppState.setDebugEnabled(true);
        }
//        setProgress(new Object() {}.getClass().getEnclosingMethod().getName());
    }

    void setupTerrain() {
        terrainManager = new TerrainManager(assetManager, bulletAppState, this);
        terrainManager.initialize();
//        setProgress(new Object(){}.getClass().getEnclosingMethod().getName());
    }

    void setupSound() {
        soundManager = new SoundManager(assetManager);
        soundManager.initialize();
//        setProgress(new Object(){}.getClass().getEnclosingMethod().getName());
    }

    void setupScreenCapture() {
        ScreenshotAppState screenShotState = new ScreenshotAppState();
        stateManager.attach(screenShotState);
        setProgress(new Object(){}.getClass().getEnclosingMethod().getName());
    }

    public void setProgress(final String loadingText) {
        if(loadingText.equals("Loading complete"))
            progress = 1f;
        else
            progress += 0.1;

        //Since this method is called from another thread, we enqueue the
        //changes to the progressbar to the update loop thread.
        enqueue(() -> {
            final int MIN_WIDTH = 32;
            int pixelWidth = (int) (MIN_WIDTH + (progressBarElement.getParent().getWidth() - MIN_WIDTH) * progress) * 2;
            if(pixelWidth > progressBarElement.getParent().getWidth())
                pixelWidth = progressBarElement.getParent().getWidth();
            progressBarElement.setConstraintWidth(new SizeValue(pixelWidth + "px"));
            progressBarElement.getParent().layoutElements();

            textRenderer.setText(loadingText);

            return null;
        });
    }

    @Override
    public SoundManagerInterface getSoundManager() {
        return soundManager;
    }

    @Override
    public TerrainManagerInterface getTerrainManager() {
        return terrainManager;
    }

    @Override
    public void setTerrainManager(TerrainManagerInterface terrainManager) {
        this.terrainManager = terrainManager;
    }

    @Override
    public String getVersion() {
        return version;
    }
}
