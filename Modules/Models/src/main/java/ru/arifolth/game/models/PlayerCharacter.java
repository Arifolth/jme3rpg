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
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

public class PlayerCharacter extends NinjaCharacter {
    private boolean left = false, right = false, up = false, down = false,
        attacking = false, capture_mouse = true, running = false, blocking = false, block_pressed = false,
        jumping = false, jump_pressed = false, attack_pressed = false;
    private final Vector3f walkDirection = new Vector3f();
    private float airTime = 0;
    private float actionTime = 0;
    private HealthBar healthBar;
    private Camera cam;

    public PlayerCharacter() {
        this.setModel("Models/Ninja/Ninja.j3o");
        this.setName(this.getClass().getName());
    }

    @Override
    protected void initializeHealthBar() {
        healthBar = new HealthBar(assetManager, getNode());
        healthBar.create();
    }

    public void setCam(Camera cam) {
        this.cam = cam;
    }

    public void block() {
        //TODO: Show Blocking animation only in case attack is coming, do nothing otherwise
        getAttackChannel().setAnim(AnimConstants.BLOCK, 0.1f);
        //TODO: ADD Blocking event
        getAttackChannel().setLoopMode(LoopMode.DontLoop);
        getAttackChannel().setSpeed(1f);
        getAttackChannel().setTime(getAttackChannel().getAnimMaxTime() / 2);
        setActionTime(getAttackChannel().getAnimMaxTime() / 2);

        playSwordSound(getSwordBlockNode(), SWORD_BLOCK);
    }

    public void attack() {
        getAttackChannel().setAnim(AnimConstants.ATTACK, 0.1f);
        getAttackChannel().setLoopMode(LoopMode.DontLoop);
        getAttackChannel().setSpeed(1f);
        setActionTime(getAttackChannel().getAnimMaxTime());

        playSwordSound(getSwordSwingNode(), SWORD_SWING);
    }

    @Override
    public void onAnimCycleDone(AnimControl ctrl, AnimChannel ch, String name) {
        if(name.equals(AnimConstants.ATTACK) && attacking && !attack_pressed) {
            if (!ch.getAnimationName().equals(AnimConstants.IDLE)) {
                ch.setAnim(AnimConstants.IDLE, 0f);
                ch.setLoopMode(LoopMode.Loop);
                ch.setSpeed(1f);
                setAttacking(false);
            }
        } else if(name.equals(AnimConstants.BLOCK) && blocking && !block_pressed) {
            if (!ch.getAnimationName().equals(AnimConstants.IDLE)) {
                ch.setAnim(AnimConstants.IDLE, 0f);
                ch.setLoopMode(LoopMode.Loop);
                ch.setSpeed(1f);
                setBlocking(false);
            }
        } else if(name.equals(AnimConstants.JUMP)) {
            setJump_pressed(false);
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


    public void movementUpdate(float k) {
        float movement_amount = 0.3f;
        if(this.isRunning()) {
            movement_amount *= 1.75;
        }

        // Gets forward direction and moves it forward
        Vector3f camDir = this.getCam().getDirection().clone().multLocal(movement_amount);
        // Gets left direction and moves it to the left
        Vector3f camLeft = this.getCam().getLeft().clone().multLocal(movement_amount * 0.75f);

        // We don't want to fly or go underground
        camDir.y = 0;
        camLeft.y = 0;

        this.getWalkDirection().set(0, 0, 0); // The walk direction is initially null

        if(this.isUp()) {
            this.getWalkDirection().addLocal(camDir);

            if(this.isLeft()) {
                this.getWalkDirection().addLocal(camLeft);
            } else if(this.isRight()) {
                this.getWalkDirection().addLocal(camLeft.negate());
            }
        } else if(this.isDown()) {
            this.getWalkDirection().addLocal(camDir.negate());

            if(this.isLeft()) {
                this.getWalkDirection().addLocal(camLeft);
            } else if(this.isRight()) {
                this.getWalkDirection().addLocal(camLeft.negate());
            }
        } else if(this.isLeft()) {
            this.getWalkDirection().addLocal(camLeft);
        } else if(this.isRight()) {
            this.getWalkDirection().addLocal(camLeft.negate());
        }

        if(!this.getCharacterControl().onGround()) {
            this.setAirTime(this.getAirTime() + k);
        } else {
            this.setAirTime(0);
            this.setJumping(false);
        }

        if (this.getAirTime() > 0.1f || this.isJump_pressed()) {
            this.setJumping(true);
            // Stop movement if jumping while walking
            if(this.isJump_pressed() && this.getAnimationChannel().getAnimationName().equals(AnimConstants.WALK))
                if (!this.getAnimationChannel().getAnimationName().equals(AnimConstants.JUMP)) {
                    this.getAnimationChannel().setAnim(AnimConstants.JUMP);
                    this.getAnimationChannel().setSpeed(1f);
                    this.getAnimationChannel().setLoopMode(LoopMode.DontLoop);
                }
            if(this.getAnimationChannel().getTime() >= 0.32f) { // Delay jump to make the animation look decent
                this.getCharacterControl().jump();
            }
        }

        if(!this.isJumping()) {
            if ((this.isUp() || this.isDown() || this.isLeft() || this.isRight())) {
                //set the walking animation
                this.getAnimationChannel().setLoopMode(LoopMode.Loop);
                if (!this.getAnimationChannel().getAnimationName().equals(AnimConstants.WALK)) {
                    this.getAnimationChannel().setAnim(AnimConstants.WALK, 0.5f);
                }
                if (this.isRunning()) {
                    this.getAnimationChannel().setSpeed(1.75f);
                }
                else {
                    this.getAnimationChannel().setSpeed(1f);
                }
                this.getPlayerStepsNode(this.isRunning()).play();
            } else if (this.getWalkDirection().length() == 0) {
                this.getAnimationChannel().setLoopMode(LoopMode.Loop);
                if (!this.getAnimationChannel().getAnimationName().equals(AnimConstants.IDLE)) {
                    this.getAnimationChannel().setAnim(AnimConstants.IDLE, 0f);
                    this.getAnimationChannel().setSpeed(1f);
                }
                this.getPlayerStepsNode(false).pause();
            }
        } else {
            this.getPlayerStepsNode(false).pause();
        }

        if(this.getActionTime() > 0) {
            this.setActionTime(this.getActionTime() - k);
        }

        if(this.isBlocking()) {
            if (this.getActionTime() <= 0 && !this.getAttackChannel().getAnimationName().equals(AnimConstants.BLOCK)) {
                this.block();
            }
            if(!this.isBlock_pressed() && this.getActionTime() <= 0) {
                this.getAttackChannel().setAnim(AnimConstants.IDLE, 0f);
                this.getAttackChannel().setSpeed(1f);
                this.setBlocking(false);
            }
        } else if(this.isAttacking()) {
            if (this.getActionTime() <= 0 && !this.getAttackChannel().getAnimationName().equals(AnimConstants.ATTACK)) {
                this.attack();
            }
            if(!this.isAttack_pressed() && this.getActionTime() <= 0) {
                this.getAttackChannel().setAnim(AnimConstants.IDLE, 0f);
                this.getAttackChannel().setSpeed(1f);
                this.setAttacking(false);
            }
        }

        this.getCharacterControl().setWalkDirection(this.getWalkDirection());

        // Rotate model to point walk direction if moving
        if((this.getWalkDirection().length() != 0) && (this.isUp() || this.isLeft() || this.isRight()))
            this.getCharacterControl().setViewDirection(this.getWalkDirection().negate());
        // negating cause the model is flipped

        //walk backwards
        if((this.getWalkDirection().length() != 0) && this.isDown())
            this.getCharacterControl().setViewDirection(this.getWalkDirection());
    }

    private void healthBarUpdate() {
        healthBar.update();
    }

    @Override
    public boolean isAttacking() {
        return attacking;
    }

    @Override
    public boolean isBlocking() {
        return blocking;
    }

    public Vector3f getWalkDirection() {
        return walkDirection;
    }

    public boolean isLeft() {
        return left;
    }

    public boolean isRight() {
        return right;
    }

    public boolean isUp() {
        return up;
    }

    public boolean isDown() {
        return down;
    }

    public boolean isCapture_mouse() {
        return capture_mouse;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isBlock_pressed() {
        return block_pressed;
    }

    public boolean isJumping() {
        return jumping;
    }

    public boolean isJump_pressed() {
        return jump_pressed;
    }

    public boolean isAttack_pressed() {
        return attack_pressed;
    }

    public float getAirTime() {
        return airTime;
    }

    public float getActionTime() {
        return actionTime;
    }

    public Camera getCam() {
        return cam;
    }

    public void setAttacking(boolean attacking) {
        this.attacking = attacking;
    }

    public void setBlocking(boolean blocking) {
        this.blocking = blocking;
    }

    public void setAttack_pressed(boolean attack_pressed) {
        this.attack_pressed = attack_pressed;
    }

    public void setLeft(boolean left) {
        this.left = left;
    }

    public void setRight(boolean right) {
        this.right = right;
    }

    public void setUp(boolean up) {
        this.up = up;
    }

    public void setDown(boolean down) {
        this.down = down;
    }

    public void setJumping(boolean jumping) {
        this.jumping = jumping;
    }

    public void setBlock_pressed(boolean block_pressed) {
        this.block_pressed = block_pressed;
    }

    public void setJump_pressed(boolean jump_pressed) {
        this.jump_pressed = jump_pressed;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public void setActionTime(float actionTime) {
        this.actionTime = actionTime;
    }

    public void setAirTime(float airTime) {
        this.airTime = airTime;
    }
}
