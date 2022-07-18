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

import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.ChaseCamera;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.*;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.ui.Picture;
import ru.arifolth.anjrpg.weather.Emitter;
import ru.arifolth.anjrpg.weather.RainEmitter;
import ru.arifolth.game.CharacterInterface;
import ru.arifolth.game.Constants;
import ru.arifolth.game.InitializationDelegateInterface;
import ru.arifolth.game.Utils;
import ru.arifolth.game.models.NonPlayerCharacter;
import ru.arifolth.game.models.PlayerCharacter;

import java.util.Map;
import java.util.stream.IntStream;

import static ru.arifolth.anjrpg.GameLogicCore.RAY_DOWN;

public class InitializationDelegate implements InitializationDelegateInterface {
    private final GameLogicCore gameLogicCore;

    public InitializationDelegate(GameLogicCore gameLogicCore) {
        this.gameLogicCore = gameLogicCore;
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
        Emitter emitter = new RainEmitter(gameLogicCore.getRootNode(), gameLogicCore.getAssetManager());
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
        this.setupGameOverIndicator();

        //put player at the beginning location
        this.initializePlayer(positionCharacters);

        //position NPCs around the Player
        this.initializeNPCs(positionCharacters);
    }

    @Override
    public void setupCamera() {
        // We re-use the flyby camera for rotation, while positioning is handled by physics
        //flyCam.setMoveSpeed(10);
        gameLogicCore.getFlyCam().setMoveSpeed(100);
        //change (increase) view distance
        gameLogicCore.getCam().setFrustumFar(10000);
        gameLogicCore.getCam().onFrameChange();

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
        chaseCam.setToggleRotationTrigger(new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));

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
    public void setupTrees() {
        final Spatial treeModel = gameLogicCore.getAssetManager().loadModel("Models/Fir1/fir1_androlo.j3o");

        for(int i = 0; i < Utils.getRandomNumberInRange(2000, 2001); i++) {
            Spatial treeModelCustom = treeModel.clone();
            treeModelCustom.scale(1 + Utils.getRandomNumberInRange(1, 10), 1 + Utils.getRandomNumberInRange(1, 10), 1 + Utils.getRandomNumberInRange(1, 10));

            gameLogicCore.getForestNode().attachChild(treeModelCustom);
        }
    }

    @Override
    public void positionTrees(TerrainQuad quad) {
        if(quad.getUserData("quadForest") != null)
            return;

        for(Spatial treeNode: gameLogicCore.getForestNode().getChildren()) {
            CollisionResults results = new CollisionResults();

            Vector3f start = new Vector3f(gameLogicCore.getPlayerCharacter().getCharacterControl().getPhysicsLocation().x + Utils.getRandomNumberInRange(-1000, 1000), gameLogicCore.getPlayerCharacter().getCharacterControl().getPhysicsLocation().y, gameLogicCore.getPlayerCharacter().getCharacterControl().getPhysicsLocation().z + Utils.getRandomNumberInRange(-1000, 1000));
            Ray ray = new Ray(start, RAY_DOWN);

            quad.collideWith(ray, results);
            CollisionResult hit = results.getClosestCollision();
            if(hit != null) {
                if(hit.getContactPoint().y <= Constants.WATER_LEVEL_HEIGHT) {
                    continue;
                }
                Vector3f plantLocation = new Vector3f(hit.getContactPoint().x, hit.getContactPoint().y, hit.getContactPoint().z);
                treeNode.setLocalTranslation(plantLocation.x, plantLocation.y, plantLocation.z);
                treeNode.setLocalRotation(new Quaternion().fromAngleAxis(Utils.getRandomNumberInRange(-6.5f, 6.5f) * FastMath.DEG_TO_RAD, new Vector3f(1, 0, 1)));
            }
        }

        quad.setUserData("quadForest", true);
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
                System.out.println(adjustedPos.normalize());
                Ray ray = new Ray(adjustedPos, RAY_DOWN);

                gameLogicCore.getTerrainManager().getTerrain().collideWith(ray, results);
                CollisionResult hit = results.getClosestCollision();
                if (hit != null) {
                    Vector3f npcStartLoc = new Vector3f(hit.getContactPoint().x, hit.getContactPoint().y + Constants.MODEL_ADJUSTMENT, hit.getContactPoint().z);
                    System.out.println(npcStartLoc.normalize());
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