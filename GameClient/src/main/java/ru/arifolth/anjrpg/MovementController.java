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

package ru.arifolth.anjrpg;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.input.InputManager;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.InputListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.input.controls.Trigger;
import com.jme3.system.AppSettings;
import ru.arifolth.anjrpg.interfaces.InitStateEnum;
import ru.arifolth.anjrpg.menu.MainMenuState;
import ru.arifolth.anjrpg.menu.MenuUtils;
import ru.arifolth.anjrpg.interfaces.BindingConstants;
import ru.arifolth.anjrpg.interfaces.CharacterInterface;
import ru.arifolth.anjrpg.interfaces.MovementControllerInterface;

import static ru.arifolth.anjrpg.interfaces.BindingConstants.*;

public class MovementController implements MovementControllerInterface {
    private CharacterInterface playerCharacter;
    private Application app;
    private InputManager inputManager;
    private AppSettings settings;

    public MovementController(Application app, InputManager inputManager) {
        this.inputManager = inputManager;
        this.app = app;
        this.settings = app.getContext().getSettings();
    }

    /** These are our custom actions triggered by key presses.
     * We do not walk yet, we just keep track of the direction the user pressed. */
    @Override
    public void onAction(String binding, boolean pressed, float tpf) {
        keyPressed(binding, pressed);
    }

    public void keyPressed(String binding, boolean pressed) {
        switch (BindingConstants.valueOf(binding)) {
            case LOCK:
                // LOCK action should only work while game is running
                if (isGameRunning() && pressed) {
                    playerCharacter.lockOnTarget();
                }
                break;
            case ESCAPE:
                // ESCAPE toggles menu regardless of game state (even during init)
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
                if (isGameRunning()) playerCharacter.setLeft(pressed);
                break;
            case RIGHT:
                if (isGameRunning()) playerCharacter.setRight(pressed);
                break;
            case UP:
                if (isGameRunning()) playerCharacter.setUp(pressed);
                break;
            case DOWN:
                if (isGameRunning()) playerCharacter.setDown(pressed);
                break;
            case JUMP:
                if (isGameRunning() && !playerCharacter.isJumping() &&
                        (playerCharacter.getStaminaBar() == null || !playerCharacter.getStaminaBar().isExhausted())) {
                    playerCharacter.setJump_pressed(pressed);
                    if(playerCharacter.isJump_pressed()) {
                        playerCharacter.setJumping(true);
                    }
                }
                break;
            case RUN:
                if (isGameRunning()) playerCharacter.setRunning(pressed);
                break;
            case BLOCK:
                if (isGameRunning() && playerCharacter.isCapture_mouse() && !playerCharacter.isJumping() &&
                        (playerCharacter.getStaminaBar() == null || !playerCharacter.getStaminaBar().isExhausted())) {
                    playerCharacter.setBlock_pressed(pressed);
                    if(playerCharacter.isBlock_pressed()) {
                        playerCharacter.setBlocking(true);
                    }
                }
                break;
            case ATTACK:
                if (isGameRunning() && playerCharacter.isCapture_mouse() && !playerCharacter.isJumping()) {
                    playerCharacter.setAttack_pressed(pressed);
                    if(playerCharacter.isAttack_pressed()) {
                        playerCharacter.setAttacking(true);
                    }
                }
                break;
        }
    }

    /** Helper to check if the game is in the RUNNING state and player character is ready. */
    private boolean isGameRunning() {
        ANJRpg game = (ANJRpg) app;
        return game.getInitStatus() == InitStateEnum.RUNNING && playerCharacter != null;
    }

    /** We over-write some navigational key mappings here, so we can
     * add physics-controlled walking and jumping
     * */
    @Override
    public void setUpKeys() {
        inputManager.deleteMapping(SimpleApplication.INPUT_MAPPING_EXIT);
        addInputMapping(ESCAPE);

        addDefaultInputMapping(LOCK);
        addDefaultInputMapping(UP);
        addDefaultInputMapping(DOWN);
        addDefaultInputMapping(LEFT);
        addDefaultInputMapping(RIGHT);
        addDefaultInputMapping(JUMP);
        addDefaultInputMapping(RUN);

        inputManager.addMapping(ATTACK.toString(), new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping(BLOCK.toString(), new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        registerInputListener(ATTACK);
        registerInputListener(BLOCK);
    }

    private void registerInputListener(BindingConstants mapping) {
        inputManager.addListener(this, mapping.toString());
    }

    @Override
    public void addDefaultInputMapping(BindingConstants mapping) {
        Integer key = (Integer) app.getContext().getSettings().get(mapping.name());
        try {
            key = (null == key) ? MenuUtils.getKey(MenuUtils.getKeyName(mapping.getDefaultName())) : key;
            addInputMapping(mapping, key);
        } catch (NoSuchFieldException|IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeInputMapping(BindingConstants mapping) {
        String mappingName = mapping.toString();

        inputManager.deleteMapping(mappingName);
    }

    @Override
    public void removeListener(InputListener listener) {
        inputManager.removeListener(listener);
    }

    @Override
    public void addListener(InputListener listener, String... mappingNames) {
        inputManager.addListener(listener, mappingNames);
    }

    @Override
    public void addMapping(String mappingName, Trigger trigger) {
        inputManager.addMapping(mappingName, trigger);
    }

    public void addInputMapping(BindingConstants mapping, Integer key) {
        String mappingName = mapping.toString();
        inputManager.addMapping(mappingName, new KeyTrigger(key));
        registerInputListener(mapping);
    }

    @Override
    public void addInputMapping(BindingConstants mapping) {
        String mappingName = mapping.toString();

        inputManager.deleteMapping(mappingName);

        Integer key = (Integer) app.getContext().getSettings().get(mappingName);
        try {
            key = (null == key) ? MenuUtils.getKey(MenuUtils.getKeyName(mappingName)) : key;
        } catch (NoSuchFieldException|IllegalAccessException e) {
            e.printStackTrace();
        }

        inputManager.addMapping(mappingName, new KeyTrigger(key));
        registerInputListener(mapping);
    }

    @Override
    public void setPlayerCharacter(CharacterInterface playerCharacter) {
        this.playerCharacter = playerCharacter;
    }
}