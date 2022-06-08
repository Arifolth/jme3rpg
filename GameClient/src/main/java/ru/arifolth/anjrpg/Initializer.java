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

import com.jme3.input.ChaseCamera;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.ui.Picture;
import ru.arifolth.anjrpg.weather.Emitter;
import ru.arifolth.anjrpg.weather.RainEmitter;
import ru.arifolth.game.Constants;
import ru.arifolth.game.models.NonPlayerCharacter;
import ru.arifolth.game.models.PlayerCharacter;

import java.util.stream.IntStream;

public class Initializer {

    private final GameLogicCore gameLogicCore;

    public Initializer(GameLogicCore gameLogicCore) {
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
        chaseCam.setToggleRotationTrigger(new MouseButtonTrigger(MouseInput.BUTTON_MIDDLE));

        //chaseCam.setDefaultDistance(40);
        //chaseCam.setDefaultHorizontalRotation(90f);
        //chaseCam.setDefaultVerticalRotation(90f);
    }
}