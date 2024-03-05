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

import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.ChaseCamera;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.*;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.ui.Picture;
import jme3tools.optimize.GeometryBatchFactory;
import ru.arifolth.anjrpg.interfaces.*;
import ru.arifolth.anjrpg.interfaces.weather.EmitterInterface;
import ru.arifolth.anjrpg.models.NonPlayerCharacter;
import ru.arifolth.anjrpg.models.PlayerCharacter;
import ru.arifolth.anjrpg.weather.RainEmitter;
import ru.arifolth.vegetation.GrassTypeEnum;
import ru.arifolth.vegetation.TreeTypeEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static ru.arifolth.anjrpg.interfaces.Constants.RAY_DOWN;

public class InitializationDelegate implements InitializationDelegateInterface {
    private final GameLogicCore gameLogicCore;
    final private static Logger LOGGER = Logger.getLogger(InitializationDelegate.class.getName());

    private final ExecutorService grassExecutorService = Executors.newSingleThreadExecutor();
    private final ExecutorService treesExecutorService = Executors.newSingleThreadExecutor();


    public InitializationDelegate(GameLogicCore gameLogicCore) {
        this.gameLogicCore = gameLogicCore;

        Runtime.getRuntime().addShutdownHook(new Thread(grassExecutorService::shutdownNow));
        Runtime.getRuntime().addShutdownHook(new Thread(treesExecutorService::shutdownNow));
    }

    void setupDamageIndicator() {
        Picture damageIndicator = new Picture("DamageIndicator");
        damageIndicator.setImage(gameLogicCore.getAssetManager(), "Textures/damageIndicator.png", true);
        damageIndicator.setWidth(((ANJRpg)gameLogicCore.getApp()).getSettings().getWidth());
        damageIndicator.setHeight(((ANJRpg)gameLogicCore.getApp()).getSettings().getHeight());
        damageIndicator.setPosition(0, 0);

        gameLogicCore.setDamageIndicator(damageIndicator);
    }

    void setupNPCs() {
        IntStream.range(gameLogicCore.getCharacterMap().size(), Constants.NPC_AMOUNT).forEach(i -> setupNPC());
    }

    void setupNPC() {
        NonPlayerCharacter nonPlayerCharacter = (NonPlayerCharacter) gameLogicCore.getCharacterFactory().createCharacter(NonPlayerCharacter.class);
        nonPlayerCharacter.setPlayerCharacter(gameLogicCore.getPlayerCharacter());
        gameLogicCore.getCharacterMap().put(nonPlayerCharacter.getNode(), nonPlayerCharacter);
    }

    void setupPlayer() {
        //create player
        PlayerCharacter playerCharacter = (PlayerCharacter) gameLogicCore.getCharacterFactory().createCharacter(PlayerCharacter.class);
        playerCharacter.setCam(gameLogicCore.getCam());
        playerCharacter.setDamageIndicator(gameLogicCore.getDamageIndicator());
        gameLogicCore.setPlayerCharacter(playerCharacter);
        gameLogicCore.getMovementController().setPlayerCharacter(playerCharacter);
    }

    void setupWeatherEffects() {
        EmitterInterface emitter = new RainEmitter(gameLogicCore.getRootNode(), gameLogicCore.getAssetManager());
        emitter.setSpatial(gameLogicCore.getPlayerCharacter().getNode());
        gameLogicCore.getWeatherEffectsSet().add(emitter);
    }

    void setupGameOverIndicator() {
        Picture gameOverIndicator = new Picture("GameOverIndicator");
        gameOverIndicator.setImage(gameLogicCore.getAssetManager(), "Interface/gameover.png", true);
        gameOverIndicator.setWidth(((ANJRpgInterface)gameLogicCore.getApp()).getSettings().getWidth());
        gameOverIndicator.setHeight(((ANJRpgInterface)gameLogicCore.getApp()).getSettings().getHeight());
        gameOverIndicator.setPosition(0, 0);
        gameOverIndicator.getMaterial().setColor("Color", new ColorRGBA(1f, 0f, 0f, .5f));

        gameLogicCore.setGameOverIndicator(gameOverIndicator);
    }

    @Override
    public void initialize(boolean positionCharacters) {
        setupGameOverIndicator();

        //put player at the beginning location
        initializePlayer(positionCharacters);

        //position NPCs around the Player
        //initializeNPCs(positionCharacters);
    }


    @Override
    public void setupCamera() {
        // We re-use the flyby camera for rotation, while positioning is handled by physics
        //flyCam.setMoveSpeed(10);
        gameLogicCore.getFlyCam().setMoveSpeed(100);
        //change (increase) view distance
        gameLogicCore.getCam().setFrustumFar(20000);

        /**/
        // Disable the default first-person cam!
        gameLogicCore.getFlyCam().setEnabled(false);

        // Enable a chase cam
        ChaseCamera chaseCam = new ChaseCamera(gameLogicCore.getCam(), gameLogicCore.getPlayerCharacter().getCharacterModel(), gameLogicCore.getInputManager());

        //Uncomment this to invert the camera's vertical rotation Axis
        chaseCam.setInvertVerticalAxis(true);

        //Uncomment this to invert the camera's horizontal rotation Axis
        //chaseCam.setInvertHorizontalAxis(true);

        //Comment this to disable smooth camera motion
        chaseCam.setSmoothMotion(true);

        //Uncomment this to disable trailing of the camera
        //WARNING, trailing only works with smooth motion enabled. It is true by default.
        //chaseCam.setTrailingEnabled(false);

        //Uncomment this to look 3 world units above the target
        //chaseCam.setLookAtOffset(Vector3f.UNIT_Y.mult(3));
        //chaseCam.setLookAtOffset(new Vector3f(0, 1, -1).mult(3));
        chaseCam.setLookAtOffset(new Vector3f(0, 3.5f, 1.5f).mult(3));

        //Uncomment this to enable rotation when the middle mouse button is pressed (like Blender)
        //WARNING : setting this trigger disable the rotation on right and left mouse button click
        chaseCam.setToggleRotationTrigger(new MouseButtonTrigger(MouseInput.BUTTON_MIDDLE));

        //chaseCam.setDefaultDistance(40);
        //chaseCam.setDefaultHorizontalRotation(90f);
        //chaseCam.setDefaultVerticalRotation(90f);
    }

    @Override
    public void initializePlayer(boolean positionCharacters) {
        this.attachPlayer();
        this.enablePlayerPhysics();
        if(positionCharacters) {
            this.initPlayerComplete();
        }
    }

    @Override
    public List<Spatial> setupTrees() {
        int forestSize = (int) Utils.getRandomNumberInRange(25, 100);
        List<Spatial> quadForest = new ArrayList<>(forestSize);
        for(int i = 0; i < forestSize; i++) {
            Spatial treeModelCustom = TreeTypeEnum.getRandomTree();
            treeModelCustom.scale(1 + Utils.getRandomNumberInRange(1, 10), 1 + Utils.getRandomNumberInRange(1, 10), 1 + Utils.getRandomNumberInRange(1, 10));
            quadForest.add(treeModelCustom);
        }

        return quadForest;
    }

    @Override
    public List<Spatial> setupGrass() {
        final int grassAmount = 5_000;
        List<Spatial> quadGrass = new ArrayList<>(grassAmount);
        for(int i = 0; i < grassAmount; i++) {
            Spatial grassInstance = GrassTypeEnum.REGULAR.getGrass();
            grassInstance.setLocalScale(1 + Utils.getRandomNumberInRange(1, 3), 1 + Utils.getRandomNumberInRange(1, 3), 1 + Utils.getRandomNumberInRange(1, 3));
            grassInstance.setLocalTranslation(grassInstance.getLocalTranslation().getX(), grassInstance.getLocalTranslation().getY(), grassInstance.getLocalTranslation().getZ() - 15);
            grassInstance.rotate(Utils.getRandomNumberInRange(-0.65f, 0.65f), Utils.getRandomNumberInRange(-1.65f, 1.65f), 0);
            quadGrass.add(grassInstance);
        }

        return quadGrass;
    }

    @Override
    public void update() {

    }

    @Override
    public void positionGrass(TerrainQuad quad) {
        for(int i = 0; i < 30; i++)
            internalPositionGrass(quad);
    }

    private void internalPositionGrass(TerrainQuad quad) {
        var context = new Object() {
            Node grassNode = quad.getUserData(Constants.QUAD_GRASS);
        };

        final Vector3f quadLocation = gameLogicCore.getPlayerCharacter().getCharacterControl().getPhysicsLocation();
        if (context.grassNode == null)
            context.grassNode = new Node();

        grassExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                List<Spatial> quadGrass = setupGrass();

                Stream<Spatial> stream = quadGrass.stream();
                stream.forEach(grassSpatial -> {
                    CollisionResults results = new CollisionResults();

                    Vector3f start = new Vector3f(quadLocation.x + Utils.getRandomNumberInRange(-2000, 2000), 70, quadLocation.z + Utils.getRandomNumberInRange(-2000, 2000));
                    Ray ray = new Ray(start, RAY_DOWN);

                    quad.collideWith(ray, results);
                    CollisionResult hit = results.getClosestCollision();
                    if (hit != null) {
                        if ((hit.getContactPoint().y > Constants.WATER_LEVEL_HEIGHT)) {
                            Vector3f plantLocation = new Vector3f(hit.getContactPoint().x, hit.getContactPoint().y, hit.getContactPoint().z);
                            grassSpatial.setLocalTranslation(plantLocation.x, plantLocation.y, plantLocation.z);

                            context.grassNode.attachChild(grassSpatial);
                        }
                    }
                });

                context.grassNode = (Node) GeometryBatchFactory.optimize(context.grassNode);
                context.grassNode.setShadowMode(RenderQueue.ShadowMode.Receive);
                context.grassNode.setQueueBucket(RenderQueue.Bucket.Transparent);
                context.grassNode.setCullHint(Spatial.CullHint.Dynamic);
                context.grassNode.updateModelBound();

                quad.setUserData(Constants.QUAD_GRASS, context.grassNode);

                gameLogicCore.getApp().enqueue(() -> {
                    gameLogicCore.getGrassNode().attachChild(context.grassNode);
                });
            }
        });
    }

    @Override
    public void positionTrees(TerrainQuad quad) {
        for(int i = 0; i < 30; i++)
            intrernalPositionTrees(quad);
    }

    private void intrernalPositionTrees(TerrainQuad quad) {
        var context = new Object() {
            Node treesNode = quad.getUserData(Constants.QUAD_FOREST);
        };

        final Vector3f quadLocation = gameLogicCore.getPlayerCharacter().getCharacterControl().getPhysicsLocation();
        if (context.treesNode == null)
            context.treesNode = new Node();

        treesExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                List<Spatial> quadForest = setupTrees();

                Stream<Spatial> stream = quadForest.stream();
                stream.forEach(treeNode -> {
                    CollisionResults results = new CollisionResults();

                    Vector3f start = new Vector3f(quadLocation.x + Utils.getRandomNumberInRange(-2000, 2000), 70, quadLocation.z + Utils.getRandomNumberInRange(-2000, 2000));
                    Ray ray = new Ray(start, RAY_DOWN);

                    quad.collideWith(ray, results);
                    CollisionResult hit = results.getClosestCollision();
                    if (hit != null) {
                        if (hit.getContactPoint().y > Constants.WATER_LEVEL_HEIGHT) {
                            Vector3f plantLocation = new Vector3f(hit.getContactPoint().x, hit.getContactPoint().y, hit.getContactPoint().z);
                            treeNode.setLocalTranslation(plantLocation.x, plantLocation.y, plantLocation.z);
                            treeNode.setLocalRotation(new Quaternion().fromAngleAxis(Utils.getRandomNumberInRange(-6.5f, 6.5f) * FastMath.DEG_TO_RAD, new Vector3f(1, 0, 1)));

                            treeNode.setLocalRotation(new Quaternion().fromAngleAxis(Utils.getRandomNumberInRange(0f, 360f) * FastMath.DEG_TO_RAD, new Vector3f(0, 1, 0)));

                            context.treesNode.attachChild(treeNode);
                        }
                    }
                });

                context.treesNode = (Node) GeometryBatchFactory.optimize(context.treesNode);
                context.treesNode.setShadowMode(RenderQueue.ShadowMode.Cast);
                context.treesNode.setCullHint(Spatial.CullHint.Dynamic);
                context.treesNode.updateModelBound();

                quad.setUserData(Constants.QUAD_FOREST, context.treesNode);

                gameLogicCore.getApp().enqueue(() -> {
                    gameLogicCore.getGrassNode().attachChild(context.treesNode);
                });
            }
        });
    }

    @Override
    public void positionPlayer() {
        CollisionResults results = new CollisionResults();

        Vector3f start = Constants.PLAYER_START_LOCATION;
        Ray ray = new Ray(start, RAY_DOWN);

        gameLogicCore.getTerrainManager().getTerrain().collideWith(ray, results);
        CollisionResult hit = results.getClosestCollision();

        Vector3f playerStartLoc = new Vector3f(hit.getContactPoint().x, hit.getContactPoint().y + Constants.MODEL_ADJUSTMENT, hit.getContactPoint().z);
        gameLogicCore.getPlayerCharacter().getCharacterControl().setPhysicsLocation(playerStartLoc);
    }

    @Override
    public void initializeNPCs(boolean positionCharacters) {
        this.setupNPCs();
        this.attachInitialNPCs();
        if(positionCharacters) {
            this.positionNPCs(gameLogicCore.getCharacterMap());
        }
        this.enableNPCsPhysics();
        if(positionCharacters) {
            this.initNPCsComplete();
        }
    }

    @Override
    public void positionNPCs(Map<Node, CharacterInterface> characterMap) {
        CharacterInterface playerCharacter = gameLogicCore.getPlayerCharacter();
        Vector3f playerPos = playerCharacter.getCharacterControl().getPhysicsLocation();
        playerPos.y = playerPos.y + 150;
        for(CharacterInterface character: characterMap.values()) {
            if(character.isInitializing()) {
                CollisionResults results = new CollisionResults();
                Vector3f adjustedPos = new Vector3f(playerPos.x + Utils.getRandomNumberInRange(-Constants.NPC_LOCATION_RANGE, Constants.NPC_LOCATION_RANGE), playerPos.y + 150, playerPos.z + Utils.getRandomNumberInRange(-Constants.NPC_LOCATION_RANGE, Constants.NPC_LOCATION_RANGE));

                Ray ray = new Ray(adjustedPos, RAY_DOWN);

                gameLogicCore.getTerrainManager().getTerrain().collideWith(ray, results);
                CollisionResult hit = results.getClosestCollision();
                if (hit != null) {
                    Vector3f npcStartLoc = new Vector3f(hit.getContactPoint().x, hit.getContactPoint().y + Constants.MODEL_ADJUSTMENT, hit.getContactPoint().z);

                    character.getCharacterControl().setPhysicsLocation(npcStartLoc);
                }
            }
        }
    }

    public void enablePlayerPhysics() {
        CharacterInterface character = gameLogicCore.getPlayerCharacter();
        if(character.isInitializing()) {
            Utils.enableEntityPhysics(character);
        }
    }

    public void enableNPCsPhysics() {
        for(CharacterInterface character: gameLogicCore.getCharacterMap().values()) {
            if(character.isInitializing()) {
                Utils.enableEntityPhysics(character);
            }
        }
    }

    public void attachPlayer() {
        gameLogicCore.getPlayerCharacter().spawn();
    }

    @Override
    public void detachNPCs() {
        Node enemies = gameLogicCore.getEnemies();
        gameLogicCore.getRootNode().detachChild(enemies);

        for(CharacterInterface character: gameLogicCore.getCharacterMap().values()) {
            character.removeCharacter();
        }
    }

    public void attachInitialNPCs() {
        Node enemies = gameLogicCore.getEnemies();
        gameLogicCore.getRootNode().attachChild(enemies);

        attachNPCs();
    }

    @Override
    public void initPlayerComplete() {
        gameLogicCore.getPlayerCharacter().setInitializing(false);
    }

    @Override
    public void initNPCsComplete() {
        for(CharacterInterface character: gameLogicCore.getCharacterMap().values()) {
            if(character.isInitializing()) {
                character.setInitializing(false);
            }
        }
    }

    public void attachNPCs() {
        for(CharacterInterface character: gameLogicCore.getCharacterMap().values()) {
            if(character.isInitializing()) {
                character.spawn();
            }
        }
    }

}