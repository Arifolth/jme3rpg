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
import com.jme3.input.controls.ActionListener;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

public class PlayerCharacter extends NinjaCharacter implements ActionListener {
    private MovementController movementController = new MovementController(this);
    private boolean left = false, right = false, up = false, down = false,
        attacking = false, capture_mouse = true, running = false, blocking = false, block_pressed = false,
        jumping = false, jump_pressed = false, attack_pressed = false;
    private final Vector3f walkDirection = new Vector3f();
    private float airTime = 0;
    private float actionTime = 0;
    private HealthBar healthBar;
    private Camera cam;

    public PlayerCharacter() {
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
        movementController.keyPressed(binding, pressed);
    }

    public void block() {
        //TODO: Show Blocking animation only in case attack is coming, do nothing otherwise
        getAttackChannel().setAnim(AnimConstants.ANIM_BLOCK, 0.1f);
        //TODO: ADD Blocking event
        getAttackChannel().setLoopMode(LoopMode.DontLoop);
        getAttackChannel().setSpeed(1f);
        getAttackChannel().setTime(getAttackChannel().getAnimMaxTime() / 2);
        setActionTime(getAttackChannel().getAnimMaxTime() / 2);

        playSwordSound(getSwordBlockNode(), SWORD_BLOCK);
    }

    public void attack() {
        getAttackChannel().setAnim(AnimConstants.ANIM_ATTACK, 0.1f);
        getAttackChannel().setLoopMode(LoopMode.DontLoop);
        getAttackChannel().setSpeed(1f);
        setActionTime(getAttackChannel().getAnimMaxTime());

        playSwordSound(getSwordSwingNode(), SWORD_SWING);
    }

    @Override
    public void onAnimCycleDone(AnimControl ctrl, AnimChannel ch, String name) {
        if(name.equals(AnimConstants.ANIM_ATTACK) && attacking && !attack_pressed) {
            if (!ch.getAnimationName().equals(AnimConstants.ANIM_IDLE)) {
                ch.setAnim(AnimConstants.ANIM_IDLE, 0f);
                ch.setLoopMode(LoopMode.Loop);
                ch.setSpeed(1f);
                setAttacking(false);
            }
        } else if(name.equals(AnimConstants.ANIM_BLOCK) && blocking && !block_pressed) {
            if (!ch.getAnimationName().equals(AnimConstants.ANIM_IDLE)) {
                ch.setAnim(AnimConstants.ANIM_IDLE, 0f);
                ch.setLoopMode(LoopMode.Loop);
                ch.setSpeed(1f);
                setBlocking(false);
            }
        } else if(name.equals(AnimConstants.ANIM_JUMP)) {
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

        movementController.movementUpdate(k);
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
