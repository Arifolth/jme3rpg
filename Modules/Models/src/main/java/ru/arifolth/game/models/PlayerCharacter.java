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

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.LoopMode;
import com.jme3.audio.AudioNode;
import com.jme3.input.controls.ActionListener;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

public class PlayerCharacter extends NinjaCharacter implements ActionListener {
    public static final float MAXIMUM_HEALTH = 75f;

    private Camera cam;
    private final Vector3f walkDirection = new Vector3f();
    private boolean left = false, right = false, up = false, down = false,
        attacking = false, capture_mouse = true, running = false, blocking = false, block_pressed = false,
        jumping = false, jump_pressed = false, attack_pressed = false;
    private float airTime = 0;
    private HealthBar healthBar;

    public PlayerCharacter() {
    }

    @Override
    protected void initializeSounds() {
        AudioNode audioNode = soundManager.getFootStepsNode();
        audioNode.setName("playerFootsteps");
        getNode().attachChild(audioNode);

        soundManager.getWindNode().play();
    }

    @Override
    protected void initializeHealthBar() {
        healthBar = new HealthBar(assetManager, getNode());
        healthBar.create();
    }

    public void setCam(Camera cam) {
        this.cam = cam;
    }

    /** These are our custom actions triggered by key presses.
     * We do not walk yet, we just keep track of the direction the user pressed. */
    public void onAction(String binding, boolean pressed, float tpf) {
        if (binding.equals("Left")) {
            left = pressed;
        }
        else if (binding.equals("Right")) {
            right = pressed;
        }
        else if (binding.equals("Up")) {
            up = pressed;
        }
        else if (binding.equals("Down")) {
            down = pressed;
        }
        else if (binding.equals("Jump")) {
            jump_pressed = true;
        }
        else if (binding.equals("Run")) {
            running = pressed;
        }
        else if (binding.equals("Block")) {
            if(capture_mouse && !jumping) {
                block_pressed = pressed;
                if(block_pressed) {
                    blocking = true;
                    block();
                }
            }
        } else if (binding.equals("Attack")) {
            if(capture_mouse && !jumping) {
                attack_pressed = pressed;
                if(attack_pressed) {
                    attacking = true;
                    attack();
                }
            }
        }
    }

    private void block() {
        //TODO: Show Blocking animation only in case attack is coming, do nothing otherwise
        attackChannel.setAnim("Block", 0.1f);
        //TODO: ADD Blocking event
        attackChannel.setLoopMode(LoopMode.DontLoop);
        attackChannel.setSpeed(1f);
        attackChannel.setTime(attackChannel.getAnimMaxTime()/2);

        getSwordBlockNode().play();
        getNode().detachChildNamed("swordBlock");
    }

    private void attack() {
        attackChannel.setAnim("Attack3", 0.1f);
        //TODO: ADD Attacking event
        attackChannel.setLoopMode(LoopMode.DontLoop);

        getSwordSwingNode().play();
        getNode().detachChildNamed("swordSwing");
    }

    @Override
    public void onAnimCycleDone(AnimControl ctrl, AnimChannel ch, String name) {
        if(name.equals("Attack3") && attacking && !attack_pressed) {
            if (!ch.getAnimationName().equals("Idle3")) {
                ch.setAnim("Idle3", 0f);
                ch.setLoopMode(LoopMode.Loop);
                ch.setSpeed(1f);
                attacking = false;
            }
        } else if(name.equals("Block") && blocking && !block_pressed) {
            if (!ch.getAnimationName().equals("Idle3")) {
                ch.setAnim("Idle3", 0f);
                ch.setLoopMode(LoopMode.Loop);
                ch.setSpeed(1f);
                blocking = false;
            }
        } else if(name.equals("JumpNoHeight")) {
            jump_pressed = false;
        }
    }

    /**
     * This is the main event loop--walking happens here.
     * We check in which direction the playerControl is walking by interpreting
     * the camera direction forward (camDir) and to the side (camLeft).
     * The setWalkDirection() command is what lets a physics-controlled playerControl walk.
     * We also make sure here that the camera moves with playerControl.
     */
    @Override
    public void simpleUpdate(float k) {
        healthBarUpdate();

        movementUpdate(k);
    }

    private void healthBarUpdate() {
        healthBar.update();
    }

    private AudioNode getSwordBlockNode() {
        getNode().detachChildNamed("swordBlock");

        AudioNode audioNode = soundManager.getSwordBlockNode();
        audioNode.setName("swordBlock");
        getNode().attachChild(audioNode);
        return audioNode;
    }

    private AudioNode getSwordSwingNode() {
        AudioNode audioNode = soundManager.getSwordSwingNode();
        audioNode.setName("swordSwing");
        getNode().attachChild(audioNode);
        return audioNode;
    }

    private AudioNode getPlayerStepsNode(boolean running) {
        AudioNode playerStepsNode = (AudioNode) (getNode().getChild("playerFootsteps"));
        if (!running) {
            playerStepsNode.setPitch(0.65f);
        } else {
            playerStepsNode.setPitch(1.05f);
        }
        return playerStepsNode;
    }

    private void movementUpdate(float k) {
        float movement_amount = 0.3f;
        if(running) {
            movement_amount *= 1.75;
        }

        // Gets forward direction and moves it forward
        Vector3f camDir = cam.getDirection().clone().multLocal(movement_amount);
        // Gets left direction and moves it to the left
        Vector3f camLeft = cam.getLeft().clone().multLocal(movement_amount * 0.75f);

        // We don't want to fly or go underground
        camDir.y = 0;
        camLeft.y = 0;

        walkDirection.set(0, 0, 0); // The walk direction is initially null

        if(left) { walkDirection.addLocal(camLeft); }
        if(right) { walkDirection.addLocal(camLeft.negate()); }
        if(up) { walkDirection.addLocal(camDir);
        }
        if(down) { walkDirection.addLocal(camDir.negate()); }

        if(!characterControl.onGround()) airTime += k;
        else {
            airTime = 0;
            jumping = false;
        }


        if (airTime > 0.1f || jump_pressed) {
            jumping = true;
            // Stop movement if jumping while walking
            if(jump_pressed && animationChannel.getAnimationName().equals("Walk"))
                if (!animationChannel.getAnimationName().equals("JumpNoHeight")) {
                    animationChannel.setAnim("JumpNoHeight");
                    animationChannel.setSpeed(1f);
                    animationChannel.setLoopMode(LoopMode.DontLoop);
                }
            if(animationChannel.getTime() >= 0.32f) { // Delay jump to make the animation look decent
                characterControl.jump();
            }
        }

        if(!jumping) {
            if ((up || down)) {
                //set the walking animation
                animationChannel.setLoopMode(LoopMode.Loop);
                if (!animationChannel.getAnimationName().equals("Walk"))
                    animationChannel.setAnim("Walk", 0.5f);
                if (running) animationChannel.setSpeed(1.75f);
                else animationChannel.setSpeed(1f);
                getPlayerStepsNode(running).play();
            } else if (walkDirection.length() == 0) {
                animationChannel.setLoopMode(LoopMode.Loop);
                if (!animationChannel.getAnimationName().equals("Idle3")) {
                    animationChannel.setAnim("Idle3", 0f);
                    animationChannel.setSpeed(1f);
                }
                getPlayerStepsNode(false).pause();
            }
        } else {
            getPlayerStepsNode(false).pause();
        }

        if(blocking) {
            if (!attackChannel.getAnimationName().equals("Block")) {
                attackChannel.setAnim("Block");
                attackChannel.setSpeed(1f);
                attackChannel.setLoopMode(LoopMode.DontLoop);
            }
            if(!block_pressed) {
                attackChannel.setAnim("Idle3", 0f);
                attackChannel.setSpeed(1f);
                blocking = false;
            }
        } else if(attacking) {
            if (!attackChannel.getAnimationName().equals("Attack3")) {
                attackChannel.setAnim("Attack3");
                attackChannel.setSpeed(1f);
                attackChannel.setLoopMode(LoopMode.DontLoop);
            }
            if(!attack_pressed) {
                attackChannel.setAnim("Idle3", 0f);
                attackChannel.setSpeed(1f);
                attacking = false;
            }
        }

        characterControl.setWalkDirection(walkDirection);

        // Rotate model to point walk direction if moving
        if(walkDirection.length() != 0)
            characterControl.setViewDirection(walkDirection.negate());
        // negating cause the model is flipped

        //walk backwards
        if((walkDirection.length() != 0) && down)
            characterControl.setViewDirection(walkDirection);
    }

    @Override
    public boolean isAttacking() {
        return attacking;
    }

    @Override
    public boolean isBlocking() {
        return blocking;
    }
}
