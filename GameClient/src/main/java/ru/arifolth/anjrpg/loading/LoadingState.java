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

package ru.arifolth.anjrpg.loading;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.ui.Picture;
import ru.arifolth.anjrpg.bars.PlayerBarsState;
import ru.arifolth.anjrpg.compass.CompassState;
import ru.arifolth.anjrpg.interfaces.ANJRpgInterface;
import ru.arifolth.anjrpg.interfaces.GameLogicCoreInterface;
import ru.arifolth.anjrpg.interfaces.InitializationDelegateInterface;
import ru.arifolth.anjrpg.interfaces.LoadingProgressSink;
import ru.arifolth.anjrpg.interfaces.RolePlayingGameInterface;
import ru.arifolth.anjrpg.worldmap.WorldMapState;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Owns the loading screen and drives the loading sequence that used to live
 * in {@code ANJRpg.simpleUpdate(float)}. The screen is a plain guiNode
 * subtree (background picture or solid quad, progress bar, status text)
 * that sits on top of the world while it is being assembled.
 *
 * Everything runs on the render thread: {@code loadResources()} is invoked
 * once from {@link #update(float)} at the same logical point the old
 * PENDING/started branch called it, and {@code setProgress(String)} calls
 * made by {@code RolePlayingGame} are routed straight into this state via
 * {@link LoadingProgressSink}. Any exception during loading propagates and
 * kills the JVM - there is no error UI by design.
 */
public class LoadingState extends BaseAppState implements LoadingProgressSink {

    private static final Logger LOGGER = Logger.getLogger(LoadingState.class.getName());
    /**
     * Folder with the loading background screenshots, as an asset key
     * relative to the {@code assets} root the asset manager is registered
     * with. It points at
     * {@code Assets/src/main/resources/assets/Textures/LoadingBackground}
     * in the Assets module, so the backgrounds travel inside the game
     * and keep working in a distributed build - there is no dependency
     * on a folder next to the installation any more.
     */
    private static final String SCREENSHOTS_DIR = "Textures/LoadingBackground";

    /**
     * Classpath root the asset manager is registered with - the Assets
     * module packs its resources under this folder.
     */
    private static final String CLASSPATH_ASSETS_ROOT = "assets/";

    private static final String[] IMAGE_EXTENSIONS = {".png", ".jpg", ".jpeg"};

    /**
     * Z offset applied to the whole loading layer. The GUI render bucket
     * sorts geometries by their world Z (GuiComparator), not by child order:
     * the compass and the player bars sit at z=1 and the world map at z=5,
     * so the loading screen must float above all of them to stay in front.
     */
    private static final float LOADING_LAYER_Z = 10f;

    private enum Phase { AWAIT_LOAD, WAIT_PHYSICS, FINALIZE, DONE }

    private SimpleApplication simpleApp;
    private ANJRpgInterface game;
    private GameLogicCoreInterface gameLogicCore;

    private Node loadingRoot;
    private LoadingProgressBar progressBar;
    private BitmapText loadingText;

    private Phase phase = Phase.AWAIT_LOAD;

    @Override
    protected void initialize(Application app) {
        this.simpleApp     = (SimpleApplication) app;
        this.game          = (ANJRpgInterface) app;
        this.gameLogicCore = game.getGameLogicCore();

        loadingRoot = new Node("LoadingRoot");

        attachBackground();
        attachProgressBar();
        attachText();

        // Lift the whole layer above the HUD (see LOADING_LAYER_Z).
        loadingRoot.setLocalTranslation(0f, 0f, LOADING_LAYER_Z);

        simpleApp.getGuiNode().attachChild(loadingRoot);

        ((RolePlayingGameInterface) app).setProgressSink(this);
    }

    private void attachBackground() {
        String screenshot = pickRandomScreenshot();
        if (screenshot != null) {
            Picture bg = new Picture("LoadingBackground");
            // Loaded the same way PressAnyKeyState loads its background.
            bg.setImage(simpleApp.getAssetManager(), screenshot, false);
            bg.setWidth(simpleApp.getCamera().getWidth());
            bg.setHeight(simpleApp.getCamera().getHeight());
            bg.setPosition(0, 0);
            loadingRoot.attachChild(bg);
        } else {
            Geometry bg = new Geometry("LoadingBackground",
                    new Quad(simpleApp.getCamera().getWidth(),
                             simpleApp.getCamera().getHeight()));
            Material mat = new Material(simpleApp.getAssetManager(),
                    "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", new ColorRGBA(0.1f, 0.1f, 0.1f, 1f));
            bg.setMaterial(mat);
            loadingRoot.attachChild(bg);
        }
    }

    private void attachProgressBar() {
        progressBar = new LoadingProgressBar(simpleApp.getAssetManager(), simpleApp);
        progressBar.init();
        loadingRoot.attachChild(progressBar.getContainer());
    }

    private void attachText() {
        BitmapFont font = simpleApp.getAssetManager()
                .loadFont("Interface/Fonts/Default.fnt");
        loadingText = new BitmapText(font);
        loadingText.setSize(24);
        loadingText.setText("Loading...");
        centerText();
        loadingRoot.attachChild(loadingText);
    }

    private void centerText() {
        float tw = loadingText.getLineWidth();
        float x  = (simpleApp.getCamera().getWidth() - tw) * 0.5f;
        float y  = simpleApp.getCamera().getHeight() / 3f * 0.5f + 40f;
        loadingText.setLocalTranslation(x, y, 2f);
    }

    @Override
    public void update(float tpf) {
        switch (phase) {
            case AWAIT_LOAD:
                ((RolePlayingGameInterface) getApplication()).loadResources();
                phase = Phase.WAIT_PHYSICS;
                break;

            case WAIT_PHYSICS: {
                BulletAppState bullet =
                        ((RolePlayingGameInterface) getApplication()).getBulletAppState();
                if (bullet.getPhysicsSpace().getRigidBodyList().size()
                        == game.getTerrainManager().getRigidBodiesSize()) {

                    InitializationDelegateInterface init =
                            gameLogicCore.getInitializationDelegate();
                    init.positionPlayer();
                    init.initPlayerComplete();
                    init.positionNPCs(gameLogicCore.getCharacterMap());
                    init.initNPCsComplete();

                    //hardcoded test quest, until quests are started by NPC dialogue
                    gameLogicCore.getQuestInitialization().startQuest("test_quest");

                    getStateManager().attach(new CompassState());
                    getStateManager().attach(new PlayerBarsState());
                    getStateManager().attach(new WorldMapState(gameLogicCore));

                    // Keep loading screen on top of the newly attached HUD.
                    simpleApp.getGuiNode().detachChild(loadingRoot);
                    simpleApp.getGuiNode().attachChild(loadingRoot);

                    onProgress(1f, "Loading complete");
                    phase = Phase.FINALIZE;
                }
                break;
            }

            case FINALIZE:
                game.onLoadingComplete();
                getStateManager().detach(this);
                phase = Phase.DONE;
                break;

            case DONE:
                break;
        }
    }

    @Override
    public void onProgress(float fraction, String message) {
        progressBar.setProgress(fraction);
        loadingText.setText(message);
        centerText();
    }

    /**
     * Picks a random screenshot from the {@code Textures/LoadingBackground}
     * folder shipped in the Assets module. The folder is located through
     * the classpath (see {@link #listClasspathImages(String)}), so the
     * pictures load the same way in the IDE and in a distributed build.
     * A missing, empty or unreadable folder only degrades the background
     * to a solid color - it is not an asset-loading failure and must not
     * crash the game.
     *
     * @return the asset key of the picture to show, or {@code null} when
     *         no picture was found
     */
    private String pickRandomScreenshot() {
        List<String> candidates = listClasspathImages(SCREENSHOTS_DIR);

        if (candidates.isEmpty()) {
            LOGGER.warning("No loading screenshots found in '" + SCREENSHOTS_DIR
                    + "'. Using solid background.");
            return null;
        }

        return candidates.get(new Random().nextInt(candidates.size()));
    }

    /**
     * Lists the asset keys of every picture inside {@code assetDir}. The
     * asset manager cannot enumerate folders, so the classpath is scanned
     * directly: the game registers its assets under the {@code assets}
     * classpath root (see {@code RolePlayingGame.setupAssetManager()}) and
     * the Assets module packs that folder both as an exploded directory
     * (IDE / Maven run) and as a jar entry (packaged distribution).
     * Subfolders are scanned recursively, so the screenshots can stay
     * grouped the same way as before.
     */
    private List<String> listClasspathImages(String assetDir) {
        Set<String> keys = new TreeSet<>();
        try {
            Enumeration<URL> locations = getClass().getClassLoader()
                    .getResources(CLASSPATH_ASSETS_ROOT + assetDir);
            while (locations.hasMoreElements()) {
                URL url = locations.nextElement();
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    collectFileImages(new File(url.toURI()), assetDir, keys);
                } else if ("jar".equals(protocol)) {
                    collectJarImages((JarURLConnection) url.openConnection(),
                            assetDir, keys);
                } else {
                    LOGGER.warning("Unsupported classpath location for loading "
                            + "screenshots: " + url);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to list loading screenshots in '"
                    + assetDir + "'", e);
        }
        return new ArrayList<>(keys);
    }

    /**
     * Collects the pictures of an exploded ({@code file:}) classpath folder.
     */
    private void collectFileImages(File dir, String assetDir, Set<String> keys) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                collectFileImages(file, assetDir + "/" + file.getName(), keys);
            } else if (isImageName(file.getName())) {
                keys.add(assetDir + "/" + file.getName());
            }
        }
    }

    /**
     * Collects the pictures packed inside a jar on the classpath. The jar
     * is opened through a private handle, so closing it never affects the
     * handle the class loader keeps for itself.
     */
    private void collectJarImages(JarURLConnection connection, String assetDir,
                                  Set<String> keys)
            throws IOException, URISyntaxException {
        String prefix = CLASSPATH_ASSETS_ROOT + assetDir + "/";
        try (JarFile jar =
                     new JarFile(new File(connection.getJarFileURL().toURI()))) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                String entry = entries.nextElement().getName();
                if (entry.startsWith(prefix) && isImageName(entry)) {
                    keys.add(assetDir + "/" + entry.substring(prefix.length()));
                }
            }
        }
    }

    private static boolean isImageName(String name) {
        String lowerCase = name.toLowerCase(Locale.ROOT);
        for (String extension : IMAGE_EXTENSIONS) {
            if (lowerCase.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void cleanup(Application app) {
        ((RolePlayingGameInterface) app).setProgressSink(null);
        if (loadingRoot != null) {
            loadingRoot.removeFromParent();
        }
    }

    // BaseAppState in jME 3.9 declares these abstract; the loading screen
    // has no enabled/disabled distinction - it lives from attach to detach.
    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
    }
}
