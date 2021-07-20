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
import com.jme3.app.SimpleApplication;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import ru.arifolth.anjrpg.menu.InitStateEnum;
import ru.arifolth.anjrpg.menu.MainMenuState;
import ru.arifolth.game.models.PlayerCharacter;

import static ru.arifolth.anjrpg.BindingConstants.*;

public class MovementController implements ActionListener {
    private PlayerCharacter playerCharacter;
    private Application app;
    private InputManager inputManager;

    public MovementController(Application app, InputManager inputManager) {
        this.inputManager = inputManager;
        this.app = app;
    }

    /** These are our custom actions triggered by key presses.
     * We do not walk yet, we just keep track of the direction the user pressed. */
    public void onAction(String binding, boolean pressed, float tpf) {
        keyPressed(binding, pressed);
    }

    public void keyPressed(String binding, boolean pressed) {
        switch (BindingConstants.valueOf(binding)) {
            case ESCAPE:
                MainMenuState mainMenuState = app.getStateManager().getState(MainMenuState.class);
                if(((ANJRpg) app).getInitStatus().equals(InitStateEnum.RUNNING)) {
                    if(mainMenuState.isEnabled()) {
                        if (pressed) {
                            mainMenuState.setEnabled(false);
                        }
                    } else {
                        if(pressed){
                            mainMenuState.setEnabled(!mainMenuState.isEnabled());
                        }
                    }
                }
                break;
            case LEFT:
                playerCharacter.setLeft(pressed);
                break;
            case RIGHT:
                playerCharacter.setRight(pressed);
                break;
            case UP:
                playerCharacter.setUp(pressed);
                break;
            case DOWN:
                playerCharacter.setDown(pressed);
                break;
            case JUMP:
                playerCharacter.setJump_pressed(true);
                break;
            case RUN:
                playerCharacter.setRunning(pressed);
                break;
            case BLOCK:
                if(playerCharacter.isCapture_mouse() && !playerCharacter.isJumping()) {
                    playerCharacter.setBlock_pressed(pressed);
                    if(playerCharacter.isBlock_pressed()) {
                        playerCharacter.setBlocking(true);
                    }
                }
                break;
            case ATTACK:
                if(playerCharacter.isCapture_mouse() && !playerCharacter.isJumping()) {
                    playerCharacter.setAttack_pressed(pressed);
                    if(playerCharacter.isAttack_pressed()) {
                        playerCharacter.setAttacking(true);
                    }
                }
                break;
        }
    }


    /** We over-write some navigational key mappings here, so we can
     * add physics-controlled walking and jumping
     * */
    public void setUpKeys() {
        inputManager.deleteMapping(SimpleApplication.INPUT_MAPPING_EXIT);

        addInputMapping(ESCAPE, KeyInput.KEY_ESCAPE);

        addInputMapping(UP, KeyInput.KEY_W);
        addInputMapping(DOWN, KeyInput.KEY_S);
        addInputMapping(LEFT, KeyInput.KEY_A);
        addInputMapping(RIGHT, KeyInput.KEY_D);
        addInputMapping(JUMP, KeyInput.KEY_SPACE);
        addInputMapping(RUN, KeyInput.KEY_LSHIFT);


        inputManager.addMapping(ATTACK.toString(), new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping(BLOCK.toString(), new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        registerInputListener(ATTACK);
        registerInputListener(BLOCK);
    }

    private void registerInputListener(BindingConstants mapping) {
        inputManager.addListener(this, mapping.toString());
    }

    public void addInputMapping(BindingConstants mapping, int key) {
        String mappingName = mapping.toString();

        inputManager.deleteMapping(mappingName);
        inputManager.addMapping(mappingName, new KeyTrigger(key));

        registerInputListener(mapping);
    }

    public void setPlayerCharacter(PlayerCharacter playerCharacter) {
        this.playerCharacter = playerCharacter;
    }
}
