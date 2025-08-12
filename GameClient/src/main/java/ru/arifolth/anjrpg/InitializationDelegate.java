/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2014 - 2025 Alexander Nilov
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
import com.jme3.math.*;
import com.jme3.scene.Node;
import com.jme3.ui.Picture;
import com.simsilica.lemur.GuiGlobals;
import ru.arifolth.anjrpg.camera.DeathAwareCamera;
import ru.arifolth.anjrpg.camera.FreeFollowCamera;
import ru.arifolth.anjrpg.interfaces.*;
import ru.arifolth.anjrpg.models.NonPlayerCharacter;
import ru.arifolth.anjrpg.models.PlayerCharacter;

import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import static ru.arifolth.anjrpg.interfaces.Constants.RAY_DOWN;

public class InitializationDelegate implements InitializationDelegateInterface {
    private final GameLogicCore gameLogicCore;
    final private static Logger LOGGER = Logger.getLogger(InitializationDelegate.class.getName());
    private FreeFollowCamera freeFollowCamera;

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

        playerCharacter.getNode().setLocalRotation(new Quaternion().fromAngleAxis(FastMath.PI, Vector3f.UNIT_Y));

        gameLogicCore.setPlayerCharacter(playerCharacter);
        gameLogicCore.getMovementController().setPlayerCharacter(playerCharacter);
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
        // Disable default fly cam
        gameLogicCore.getFlyCam().setEnabled(false);

        // Create custom camera control
        freeFollowCamera = new DeathAwareCamera(
                gameLogicCore.getCam(),
                gameLogicCore.getPlayerCharacter().getCharacterModel(),
                gameLogicCore.getInputManager()
        );

        // Configure the camera
        freeFollowCamera.setOffset(new Vector3f(2.0f, 10f, 10f)); // Right shoulder position
        freeFollowCamera.setDragToRotate(false); // Free mouse look
        freeFollowCamera.setRotationSpeed(2.0f);
        freeFollowCamera.setEnabled(true);
        gameLogicCore.setFreeFollowCamera(freeFollowCamera);

        // Attach to a node in the scene (required for AbstractControl)
        Node cameraControlNode = new Node("CameraControl");
        cameraControlNode.addControl(freeFollowCamera);
        gameLogicCore.getRootNode().attachChild(cameraControlNode);

        gameLogicCore.getCam().setFrustumFar(20000);
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
    public void update() {

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
        gameLogicCore.getApp().enqueue(() -> {
            gameLogicCore.getRootNode().detachChild(enemies);
        });
        for(CharacterInterface character: gameLogicCore.getCharacterMap().values()) {
            character.removeCharacter();
        }
    }

    public void attachInitialNPCs() {
        Node enemies = gameLogicCore.getEnemies();
        gameLogicCore.getApp().enqueue(() -> {
            gameLogicCore.getRootNode().attachChild(enemies);
        });
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