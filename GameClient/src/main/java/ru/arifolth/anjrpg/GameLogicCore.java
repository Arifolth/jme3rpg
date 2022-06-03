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

import com.jme3.app.Application;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.FlyByCamera;
import com.jme3.input.InputManager;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.ui.Picture;
import ru.arifolth.anjrpg.weather.Emitter;
import ru.arifolth.game.*;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GameLogicCore implements GameLogicCoreInterface {
    private final CharacterFactory characterFactory = new CharacterFactory(this);
    private final Initializer initializer = new Initializer(this);
    private final Node enemies = new Node("enemies");

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

    private CharacterInterface playerCharacter = null;
    private Picture damageIndicator = null;
    private Map<Node, CharacterInterface> characterMap = new ConcurrentHashMap<>();
    private Set<Emitter> weatherEffectsSet = new LinkedHashSet<>();
    private Picture gameOverIndicator;

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
        initializer.setupDamageIndicator();

        initializer.setupGameOverIndicator();

        initializer.setupPlayer();

        initializer.setupNPC();

        initializer.setupCamera();

        movementController.setUpKeys();
//        initializer.setupWeatherEffects();
    }

    @Override
    public void setupPlayer() {
        initializer.setupPlayer();
    }

    @Override
    public void setupNPC() {
        initializer.setupNPC();
    }

    @Override
    public void setupCamera() {
        initializer.setupCamera();
    }

    @Override
    public void enablePhysics() {
        CharacterInterface playerCharacter = this.getPlayerCharacter();
        Utils.enableEntityPhysics(playerCharacter);

        for(CharacterInterface character: this.getCharacterMap().values()) {
            Utils.enableEntityPhysics(character);
        }
    }

    @Override
    public void attachPlayer() {
        this.getPlayerCharacter().spawn();
    }

    @Override
    public void detachNPC() {
        Node enemies = this.getEnemies();
        getRootNode().detachChild(enemies);

        for(CharacterInterface character: this.getCharacterMap().values()) {
            character.removeCharacter();
        }
    }

    @Override
    public void attachNPC() {
        Node enemies = this.getEnemies();
        getRootNode().attachChild(enemies);

        for(CharacterInterface character: this.getCharacterMap().values()) {
            character.spawn();
        }
    }

    public void reInitialize() {
        getPlayerCharacter().initializeSounds();
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

    @Override
    public void positionCharacters() {
        CollisionResults results = new CollisionResults();
        // 2. Aim the ray from cam loc to cam direction.
        Vector3f start = Constants.PLAYER_START_LOCATION;
        Ray ray = new Ray(start, new Vector3f(0, -1, 0));

        // 3. Collect intersections between Ray and Shootables in results list.
        terrainManager.getTerrain().collideWith(ray, results);
        CollisionResult hit = results.getClosestCollision();

        Vector3f playerStartLoc = new Vector3f(hit.getContactPoint().x, hit.getContactPoint().y + 3f, hit.getContactPoint().z);
        getPlayerCharacter().getCharacterControl().setPhysicsLocation(playerStartLoc);

        for(CharacterInterface character: getCharacterMap().values()) {
//            Vector3f npcStartLoc = new Vector3f(hit.getContactPoint().x + Utils.getRandomNumber(-40, 40), hit.getContactPoint().y + Utils.getRandomNumber(-40, 40), hit.getContactPoint().z);
            Vector3f npcStartLoc = new Vector3f(hit.getContactPoint().x + 40, hit.getContactPoint().y + 40, hit.getContactPoint().z);
            character.getCharacterControl().setPhysicsLocation(npcStartLoc);
        }
    }

    public void update(float tpf) {
        playerCharacter.update(tpf);

        for(CharacterInterface character : characterMap.values()) {
            character.update(tpf);
        }

        for(Emitter emitter : weatherEffectsSet) {
            emitter.update(tpf);
        }
    }

    @Override
    public Map<Node,CharacterInterface> getCharacterMap() {
        return characterMap;
    }

    public MovementControllerInterface getMovementController() {
        return movementController;
    }

    @Override
    public Set<Emitter> getWeatherEffectsSet() {
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

    public Initializer getInitializer() {
        return initializer;
    }

    @Override
    public Node getEnemies() {
        return enemies;
    }

    @Override
    public void attachGameOverIndicator() {
        rootNode.attachChild(gameOverIndicator);
    }

    @Override
    public void detachGameOverIndicator() {
        rootNode.detachChild(gameOverIndicator);
    }

    public void setGameOverIndicator(Picture gameOverIndicator) {
        this.gameOverIndicator = gameOverIndicator;
    }
}
