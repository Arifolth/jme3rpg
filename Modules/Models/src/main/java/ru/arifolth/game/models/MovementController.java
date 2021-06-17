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

package ru.arifolth.game.models;

import com.jme3.animation.LoopMode;
import com.jme3.math.Vector3f;

public class MovementController {
    public static final String BINDING_LEFT = "Left";
    public static final String BINDING_RIGHT = "Right";
    public static final String BINDING_UP = "Up";
    public static final String BINDING_DOWN = "Down";
    public static final String BINDING_JUMP = "Jump";
    public static final String BINDING_RUN = "Run";
    public static final String BINDING_BLOCK = "Block";
    public static final String BINDING_ATTACK = "Attack";
    private PlayerCharacter playerCharacter;
    public MovementController(PlayerCharacter playerCharacter) {
        this.playerCharacter = playerCharacter;
    }

    public void keyPressed(String binding, boolean pressed) {
        if (binding.equals(BINDING_LEFT)) {
            playerCharacter.setLeft(pressed);
        }
        else if (binding.equals(BINDING_RIGHT)) {
            playerCharacter.setRight(pressed);
        }
        else if (binding.equals(BINDING_UP)) {
            playerCharacter.setUp(pressed);
        }
        else if (binding.equals(BINDING_DOWN)) {
            playerCharacter.setDown(pressed);
        }
        else if (binding.equals(BINDING_JUMP)) {
            playerCharacter.setJump_pressed(true);
        }
        else if (binding.equals(BINDING_RUN)) {
            playerCharacter.setRunning(pressed);
        }
        else if (binding.equals(BINDING_BLOCK)) {
            if(playerCharacter.isCapture_mouse() && !playerCharacter.isJumping()) {
                playerCharacter.setBlock_pressed(pressed);
                if(playerCharacter.isBlock_pressed()) {
                    playerCharacter.setBlocking(true);
                }
            }
        } else if (binding.equals(BINDING_ATTACK)) {
            if(playerCharacter.isCapture_mouse() && !playerCharacter.isJumping()) {
                playerCharacter.setAttack_pressed(pressed);
                if(playerCharacter.isAttack_pressed()) {
                    playerCharacter.setAttacking(true);
                }
            }
        }
    }

    public void movementUpdate(float k) {
        float movement_amount = 0.3f;
        if(playerCharacter.isRunning()) {
            movement_amount *= 1.75;
        }

        // Gets forward direction and moves it forward
        Vector3f camDir = playerCharacter.getCam().getDirection().clone().multLocal(movement_amount);
        // Gets left direction and moves it to the left
        Vector3f camLeft = playerCharacter.getCam().getLeft().clone().multLocal(movement_amount * 0.75f);

        // We don't want to fly or go underground
        camDir.y = 0;
        camLeft.y = 0;

        playerCharacter.getWalkDirection().set(0, 0, 0); // The walk direction is initially null

        if(playerCharacter.isUp()) {
            playerCharacter.getWalkDirection().addLocal(camDir);

            if(playerCharacter.isLeft()) {
                playerCharacter.getWalkDirection().addLocal(camLeft);
            } else if(playerCharacter.isRight()) {
                playerCharacter.getWalkDirection().addLocal(camLeft.negate());
            }
        } else if(playerCharacter.isDown()) {
            playerCharacter.getWalkDirection().addLocal(camDir.negate());

            if(playerCharacter.isLeft()) {
                playerCharacter.getWalkDirection().addLocal(camLeft);
            } else if(playerCharacter.isRight()) {
                playerCharacter.getWalkDirection().addLocal(camLeft.negate());
            }
        } else if(playerCharacter.isLeft()) {
            playerCharacter.getWalkDirection().addLocal(camLeft);
        } else if(playerCharacter.isRight()) {
            playerCharacter.getWalkDirection().addLocal(camLeft.negate());
        }

        if(!playerCharacter.getCharacterControl().onGround()) {
            playerCharacter.setAirTime(playerCharacter.getAirTime() + k);
        } else {
            playerCharacter.setAirTime(0);
            playerCharacter.setJumping(false);
        }

        if (playerCharacter.getAirTime() > 0.1f || playerCharacter.isJump_pressed()) {
            playerCharacter.setJumping(true);
            // Stop movement if jumping while walking
            if(playerCharacter.isJump_pressed() && playerCharacter.getAnimationChannel().getAnimationName().equals(AnimConstants.ANIM_WALK))
                if (!playerCharacter.getAnimationChannel().getAnimationName().equals(AnimConstants.ANIM_JUMP)) {
                    playerCharacter.getAnimationChannel().setAnim(AnimConstants.ANIM_JUMP);
                    playerCharacter.getAnimationChannel().setSpeed(1f);
                    playerCharacter.getAnimationChannel().setLoopMode(LoopMode.DontLoop);
                }
            if(playerCharacter.getAnimationChannel().getTime() >= 0.32f) { // Delay jump to make the animation look decent
                playerCharacter.getCharacterControl().jump();
            }
        }

        if(!playerCharacter.isJumping()) {
            if ((playerCharacter.isUp() || playerCharacter.isDown() || playerCharacter.isLeft() || playerCharacter.isRight())) {
                //set the walking animation
                playerCharacter.getAnimationChannel().setLoopMode(LoopMode.Loop);
                if (!playerCharacter.getAnimationChannel().getAnimationName().equals(AnimConstants.ANIM_WALK)) {
                    playerCharacter.getAnimationChannel().setAnim(AnimConstants.ANIM_WALK, 0.5f);
                }
                if (playerCharacter.isRunning()) {
                    playerCharacter.getAnimationChannel().setSpeed(1.75f);
                }
                else {
                    playerCharacter.getAnimationChannel().setSpeed(1f);
                }
                playerCharacter.getPlayerStepsNode(playerCharacter.isRunning()).play();
            } else if (playerCharacter.getWalkDirection().length() == 0) {
                playerCharacter.getAnimationChannel().setLoopMode(LoopMode.Loop);
                if (!playerCharacter.getAnimationChannel().getAnimationName().equals(AnimConstants.ANIM_IDLE)) {
                    playerCharacter.getAnimationChannel().setAnim(AnimConstants.ANIM_IDLE, 0f);
                    playerCharacter.getAnimationChannel().setSpeed(1f);
                }
                playerCharacter.getPlayerStepsNode(false).pause();
            }
        } else {
            playerCharacter.getPlayerStepsNode(false).pause();
        }

        if(playerCharacter.getActionTime() > 0) {
            playerCharacter.setActionTime(playerCharacter.getActionTime() - k);
        }

        if(playerCharacter.isBlocking()) {
            if (playerCharacter.getActionTime() <= 0 && !playerCharacter.getAttackChannel().getAnimationName().equals(AnimConstants.ANIM_BLOCK)) {
                playerCharacter.block();
            }
            if(!playerCharacter.isBlock_pressed() && playerCharacter.getActionTime() <= 0) {
                playerCharacter.getAttackChannel().setAnim(AnimConstants.ANIM_IDLE, 0f);
                playerCharacter.getAttackChannel().setSpeed(1f);
                playerCharacter.setBlocking(false);
            }
        } else if(playerCharacter.isAttacking()) {
            if (playerCharacter.getActionTime() <= 0 && !playerCharacter.getAttackChannel().getAnimationName().equals(AnimConstants.ANIM_ATTACK)) {
                playerCharacter.attack();
            }
            if(!playerCharacter.isAttack_pressed() && playerCharacter.getActionTime() <= 0) {
                playerCharacter.getAttackChannel().setAnim(AnimConstants.ANIM_IDLE, 0f);
                playerCharacter.getAttackChannel().setSpeed(1f);
                playerCharacter.setAttacking(false);
            }
        }

        playerCharacter.getCharacterControl().setWalkDirection(playerCharacter.getWalkDirection());

        // Rotate model to point walk direction if moving
        if((playerCharacter.getWalkDirection().length() != 0) && (playerCharacter.isUp() || playerCharacter.isLeft() || playerCharacter.isRight()))
            playerCharacter.getCharacterControl().setViewDirection(playerCharacter.getWalkDirection().negate());
        // negating cause the model is flipped

        //walk backwards
        if((playerCharacter.getWalkDirection().length() != 0) && playerCharacter.isDown())
            playerCharacter.getCharacterControl().setViewDirection(playerCharacter.getWalkDirection());
    }
}
