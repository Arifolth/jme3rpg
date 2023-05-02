/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2014 - 2023 Alexander Nilov
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

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.input.FlyByCamera;
import com.jme3.input.InputManager;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.ui.Picture;
import ru.arifolth.anjrpg.interfaces.*;
import ru.arifolth.anjrpg.interfaces.weather.EmitterInterface;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GameLogicCore implements GameLogicCoreInterface {
    final private static Logger LOGGER = Logger.getLogger(GameLogicCore.class.getName());

    private final CharacterFactory characterFactory = new CharacterFactory(this);
    private TrackerInterface locationTracker = new LocationTracker(this);
    private final InitializationDelegate initializationDelegate = new InitializationDelegate(this);
    private final Node enemies = new Node("enemies");
    private final Node treesForestNode = new Node("Forest Node");

    private MovementControllerInterface movementController;
    private TerrainManagerInterface terrainManager;
    private Application app;
    private Camera cam;
    private FlyByCamera flyCam;
    private InputManager inputManager;
    private BulletAppState bulletAppState;
    private AssetManager assetManager;
    private Node rootNode;
    private SoundManagerInterface soundManager;
    private SkyInterface sky;
    private Picture gameOverIndicator;

    private CharacterInterface playerCharacter = null;
    private Picture damageIndicator = null;
    private Map<Node, CharacterInterface> characterMap = new ConcurrentHashMap<>();
    private Set<EmitterInterface> weatherEffectsSet = new LinkedHashSet<>();
    private GameStateManagerInterface gameStateManager = new GameStateManager(this);

    public GameLogicCore(Application app, Camera cam, FlyByCamera flyCam, InputManager inputManager, BulletAppState bulletAppState, AssetManager assetManager, SoundManagerInterface soundManager, TerrainManagerInterface terrainManager, Node rootNode) {
        this.movementController = new MovementController(app, inputManager);
        this.app = app;
        this.cam = cam;
        this.flyCam = flyCam;
        this.inputManager = inputManager;
        this.bulletAppState = bulletAppState;
        this.assetManager = assetManager;
        this.soundManager = soundManager;
        this.terrainManager = terrainManager;
        this.rootNode = rootNode;
    }

    public void initialize() {
        /*
        * Initialization order is important: first we create Player Entity and Camera, later we initialize other stuff
        * */
        initializationDelegate.setupDamageIndicator();
        initializationDelegate.setupPlayer();
        initializationDelegate.setupCamera();

        movementController.setUpKeys();
//        initializer.setupWeatherEffects();
        getRootNode().attachChild(treesForestNode);
    }

    @Override
    public Node getForestNode() {
        return treesForestNode;
    }

    public void reInitialize() {
        getPlayerCharacter().initializeSounds();

        for(CharacterInterface character: getCharacterMap().values()) {
            character.initializeSounds();
        }
    }

    public CharacterInterface getPlayerCharacter() {
        return playerCharacter;
    }

    @Override
    public Picture getDamageIndicator() {
        return damageIndicator;
    }

    @Override
    public void setDamageIndicator(Picture damageIndicator) {
        this.damageIndicator = damageIndicator;
    }

    public CharacterFactory getCharacterFactory() {
        return characterFactory;
    }

    public void setPlayerCharacter(CharacterInterface playerCharacter) {
        this.playerCharacter = playerCharacter;
    }

    public void update(float tpf) {
        playerCharacter.update(tpf);

        locationTracker.update(tpf);

        for(CharacterInterface character : characterMap.values()) {
            character.update(tpf);
        }

        for(EmitterInterface emitter : weatherEffectsSet) {
            emitter.update(tpf);
        }

        gameStateManager.update(tpf);
    }

    @Override
    public GameStateManagerInterface getGameStateManager() {
        return gameStateManager;
    }

    @Override
    public Map<Node,CharacterInterface> getCharacterMap() {
        return characterMap;
    }

    public MovementControllerInterface getMovementController() {
        return movementController;
    }

    @Override
    public Set<EmitterInterface> getWeatherEffectsSet() {
        return weatherEffectsSet;
    }

    @Override
    public BulletAppState getBulletAppState() {
        return bulletAppState;
    }

    @Override
    public Node getRootNode() {
        return rootNode;
    }

    @Override
    public AssetManager getAssetManager() {
        return assetManager;
    }

    @Override
    public SoundManagerInterface getSoundManager() {
        return soundManager;
    }

    @Override
    public Camera getCam() {
        return cam;
    }

    @Override
    public FlyByCamera getFlyCam() {
        return flyCam;
    }

    @Override
    public Application getApp() {
        return app;
    }

    @Override
    public InputManager getInputManager() {
        return inputManager;
    }

    @Override
    public Node getEnemies() {
        return enemies;
    }

    @Override
    public void attachGameOverIndicator() {
        ((SimpleApplication) app).getGuiNode().attachChild(gameOverIndicator);
    }

    @Override
    public void detachGameOverIndicator() {
        ((SimpleApplication) app).getGuiNode().detachChild(gameOverIndicator);
    }

    public void setGameOverIndicator(Picture gameOverIndicator) {
        this.gameOverIndicator = gameOverIndicator;
    }

    @Override
    public InitializationDelegateInterface getInitializationDelegate() {
        return initializationDelegate;
    }

    public TerrainManagerInterface getTerrainManager() {
        return terrainManager;
    }

    @Override
    public SkyInterface getSky() {
        return sky;
    }

    @Override
    public void setSky(SkyInterface sky) {
        this.sky = sky;
    }
}
