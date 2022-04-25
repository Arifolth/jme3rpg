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
import com.jme3.input.ChaseCamera;
import com.jme3.input.FlyByCamera;
import com.jme3.input.InputManager;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.ui.Picture;
import ru.arifolth.anjrpg.weather.Emitter;
import ru.arifolth.anjrpg.weather.RainEmitter;
import ru.arifolth.game.*;
import ru.arifolth.game.models.NonPlayerCharacter;
import ru.arifolth.game.models.PlayerCharacter;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameLogicCore implements GameLogicCoreInterface {
    private MovementControllerInterface movementController;
    private Application app;
    private Camera cam;
    private FlyByCamera flyCam;
    private InputManager inputManager;
    private BulletAppState bulletAppState;
    private AssetManager assetManager;
    private Node rootNode;
    private CharacterFactory characterFactory;
    private SoundManagerInterface soundManager;

    private CharacterInterface playerCharacter = null;
    private List<CharacterInterface> characterSet = new CopyOnWriteArrayList<>();
    private Set<Emitter> weatherEffectsSet = new LinkedHashSet<>();
    private List<DamageControlInterface> damageSet = new CopyOnWriteArrayList<>();

    public GameLogicCore(Application app, Camera cam, FlyByCamera flyCam, InputManager inputManager, BulletAppState bulletAppState, AssetManager assetManager, SoundManagerInterface soundManager, Node rootNode) {
        this.movementController = new MovementController(app, inputManager);
        this.app = app;
        this.cam = cam;
        this.flyCam = flyCam;
        this.inputManager = inputManager;
        this.bulletAppState = bulletAppState;
        this.assetManager = assetManager;
        this.soundManager = soundManager;
        this.rootNode = rootNode;
    }

    public void initialize() {
        characterFactory = new CharacterFactory(this);

        setupPlayer();

        setupNPC();

        setupCamera();

        movementController.setUpKeys();
        //setupWeatherEffects();
    }

    private void setupNPC() {
        NonPlayerCharacter nonPlayerCharacter = (NonPlayerCharacter)characterFactory.createCharacter(NonPlayerCharacter.class);
        nonPlayerCharacter.setPlayerCharacter(playerCharacter);
        characterSet.add(nonPlayerCharacter);
    }

    public void reInitialize() {
        getPlayerCharacter().initializeSounds();
    }

    private void setupWeatherEffects() {
        Emitter emitter = new RainEmitter(rootNode, assetManager);
        emitter.setSpatial(playerCharacter.getNode());
        weatherEffectsSet.add(emitter);
    }

    public CharacterInterface getPlayerCharacter() {
        return playerCharacter;
    }


    protected Picture createDamageIndicator() {
        Picture damageIndicator = new Picture("DamageIndicator");
        damageIndicator.setImage(assetManager, "Textures/damageIndicator.png", true);
        damageIndicator.setWidth(((ANJRpg)app).getSettings().getWidth());
        damageIndicator.setHeight(((ANJRpg)app).getSettings().getHeight());
        damageIndicator.setPosition(0, 0);

        rootNode.attachChild(damageIndicator);

        return damageIndicator;
    }

    private void setupPlayer() {
        //create player
        playerCharacter = (PlayerCharacter)characterFactory.createCharacter(PlayerCharacter.class);
        playerCharacter.setCam(cam);
        movementController.setPlayerCharacter(playerCharacter);

        playerCharacter.setDamageIndicator(createDamageIndicator());
    }

    public void setupCamera() {
        // We re-use the flyby camera for rotation, while positioning is handled by physics
        //flyCam.setMoveSpeed(10);
        flyCam.setMoveSpeed(100);
        //change (increase) view distance
        cam.setFrustumFar(10000);
        cam.onFrameChange();

        /**/
        // Disable the default first-person cam!
        flyCam.setEnabled(false);

        // Enable a chase cam
        ChaseCamera chaseCam = new ChaseCamera(cam, playerCharacter.getCharacterModel(), inputManager);

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

    public void update(float tpf) {
        playerCharacter.update(tpf);

        for(CharacterInterface character : characterSet) {
            character.update(tpf);
        }

        for(CharacterInterface character : characterSet) {
            character.update(tpf);
        }

        for(DamageControlInterface damageControl : damageSet) {
            damageControl.update(tpf);
        }

        for(Emitter emitter : weatherEffectsSet) {
            emitter.update(tpf);
        }
    }

    @Override
    public List<DamageControlInterface> getDamageSet() {
        return damageSet;
    }

    @Override
    public List<CharacterInterface> getCharacterSet() {
        return characterSet;
    }

    public MovementControllerInterface getMovementController() {
        return movementController;
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
}
