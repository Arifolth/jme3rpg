package ru.arifolth.anjrpg;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.input.*;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import ru.arifolth.anjrpg.character.Character;
import ru.arifolth.anjrpg.character.PlayerCharacter;
import ru.arifolth.anjrpg.character.factory.CharacterFactory;
import ru.arifolth.anjrpg.weather.Emitter;
import ru.arifolth.anjrpg.weather.SnowEmitter;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 20.12.12
 * Time: 22:21
 * To change this template use File | Settings | File Templates.
 */
public class GameLogicCore {
    private Camera cam;
    private FlyByCamera flyCam;
    private InputManager inputManager;
    private BulletAppState bulletAppState;
    private AssetManager assetManager;
    private Node rootNode;
    private CharacterFactory characterFactory;

    private PlayerCharacter playerCharacter = null;
    private Set<Character> characterSet = new LinkedHashSet<Character>();
    private Set<Emitter> weatherEffectsSet = new LinkedHashSet<Emitter>();
    
    public GameLogicCore(Camera cam, FlyByCamera flyCam, InputManager inputManager, BulletAppState bulletAppState, AssetManager assetManager, Node rootNode) {
        this.cam = cam;
        this.flyCam = flyCam;
        this.inputManager = inputManager;
        this.bulletAppState = bulletAppState;
        this.assetManager = assetManager;
        this.rootNode = rootNode;
    }

    public void initialize() {
        characterFactory = new CharacterFactory(bulletAppState, assetManager);

        setupPlayer();
        setupCamera();

        setUpKeys();
        setupWeatherEffects();

    }

    private void setupWeatherEffects() {
        Emitter snowEmitter = new SnowEmitter(rootNode, assetManager);
        snowEmitter.setSpatial(playerCharacter.getNode());
        weatherEffectsSet.add(snowEmitter);
    }

    private void setupPlayer() {
        //create player
        playerCharacter = (PlayerCharacter)characterFactory.createCharacter(PlayerCharacter.class);
        playerCharacter.setCam(cam);
        rootNode.attachChild(playerCharacter.getNode());

        characterSet.add(playerCharacter);
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
        //chaseCam.setSmoothMotion(true);

        //Uncomment this to disable trailing of the camera
        //WARNING, trailing only works with smooth motion enabled. It is true by default.
        //chaseCam.setTrailingEnabled(false);

        //Uncomment this to look 3 world units above the target
        //chaseCam.setLookAtOffset(Vector3f.UNIT_Y.mult(3));
        //chaseCam.setLookAtOffset(new Vector3f(0, 1, -1).mult(3));
        chaseCam.setLookAtOffset(new Vector3f(0, 3.5f, -1.5f).mult(3));

        //Uncomment this to enable rotation when the middle mouse button is pressed (like Blender)
        //WARNING : setting this trigger disable the rotation on right and left mouse button click
        chaseCam.setToggleRotationTrigger(new MouseButtonTrigger(MouseInput.BUTTON_MIDDLE));

        //Uncomment this to set mutiple triggers to enable rotation of the cam
        //Here spade bar and middle mouse button
        //chaseCam.setToggleRotationTrigger(new MouseButtonTrigger(MouseInput.BUTTON_MIDDLE));

        //chaseCam.setDefaultDistance(40);
        //chaseCam.setDefaultHorizontalRotation(90f);
        //chaseCam.setDefaultVerticalRotation(90f);
    }

    /** We over-write some navigational key mappings here, so we can
     * add physics-controlled walking and jumping: */
    public void setUpKeys() {
        inputManager.addMapping("Left",  new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Up",    new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Down",  new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Jump",  new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("CatchM", new KeyTrigger(KeyInput.KEY_Q));
        inputManager.addMapping("Run",    new KeyTrigger(KeyInput.KEY_LSHIFT));
        inputManager.addMapping("Attack", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(playerCharacter, "Left");
        inputManager.addListener(playerCharacter, "Right");
        inputManager.addListener(playerCharacter, "Up");
        inputManager.addListener(playerCharacter, "Down");
        inputManager.addListener(playerCharacter, "Jump");
        inputManager.addListener(playerCharacter, "CatchM");
        inputManager.addListener(playerCharacter, "Run");
        inputManager.addListener(playerCharacter, "Attack");
    }

    public void update(float tpf) {
        for(Character character : characterSet) {
            character.simpleUpdate(tpf);
        }

        for(Emitter emitter : weatherEffectsSet) {
            emitter.simpleUpdate(tpf);
        }
    }


}
