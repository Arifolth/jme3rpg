/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2014 - 2024 Alexander Nilov
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

package ru.arifolth.anjrpg.models;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.LoopMode;
import com.jme3.app.SimpleApplication;
import com.jme3.collision.CollisionResults;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.*;
import com.jme3.ui.Picture;
import ru.arifolth.anjrpg.interfaces.*;

public class PlayerCharacter extends AnimatedCharacter {
    public static final String PLAYER_CHARACTER_MODEL = "Models/Ninja/Ninja.j3o";
    protected final AnimationDelegateInterface animationDelegate = new AnimationDelegate(this);
    protected float shootDelay;
    protected float shootRate;

    private boolean left = false, right = false, up = false, down = false,
        attacking = false, capture_mouse = true, running = false, blocking = false, block_pressed = false,
        jumping = false, jump_pressed = false, attack_pressed = false;
    private final Vector3f walkDirection = new Vector3f();
    private float airTime = 0;
    private float actionTime = 0;
    private Camera cam;
    private static final float MAX_DAMAGED_TIME = Constants.SHOOT_DELAY;
    private float playerDamaged = 0f;
    private Picture damageIndicator;
    protected float firingRange;
    protected boolean dead = false;
    protected boolean initializing = true;
    private float health;

    public PlayerCharacter() {
        this.setModel(PLAYER_CHARACTER_MODEL);
        this.setName(this.getClass().getName());

        this.firingRange = Constants.MELEE_DISTANCE_LIMIT;
        this.shootDelay = Constants.SHOOT_DELAY;
        this.shootRate = Constants.SHOOT_RATE;
    }

    @Override
    public void initializeModelLod() {
        LodUtils.setUpModelLod(characterModel);
    }

    @Override
    protected void initHealthBar() {
        healthBar = new HealthBar(gameLogicCore.getAssetManager(), this);
        healthBar.create();
    }

    public void setCam(Camera cam) {
        this.cam = cam;
    }

    public void block() {
        animationDelegate.blockAnimation();

        playSwordSound(getSwordSwingNode());
    }

    public void attack() {
        animationDelegate.attackAnimation();

        playSwordSound(getSwordSwingNode());

        Node enemies = gameLogicCore.getEnemies();

        Ray ray = new Ray(characterControl.getPhysicsLocation(), characterControl.getViewDirection().negate());
        ray.setLimit(Constants.MELEE_DISTANCE_LIMIT);
        // Results of the collision test are written into this object
        CollisionResults results = new CollisionResults();

        // Test for collisions between the road and the ray
        enemies.collideWith(ray, results);
        if(results.size() > 0) {
            Geometry geometry = results.getClosestCollision().getGeometry();
            if(geometry == null)
                return;
            Node parent = geometry.getParent();
            if(parent == null)
                return;
            Node grandParent = parent.getParent();
            CharacterInterface npc = gameLogicCore.getCharacterMap().get(grandParent);
            if(npc != null) {
                boolean blocked = Utils.getRandom(Constants.HIT_PROBABILITY);
                if(!blocked) {
                    npc.getHealthBar().applyDamage(Constants.DAMAGE);
                    playSwordSound(getSwordHitNode());
                } else {
                    npc.getAnimationDelegate().blockAnimation();
                    npc.resetShootCounterByQuarter();
                    playSwordSound(getSwordBlockNode());
                }
            }
        }
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
        } else if (name.equals(AnimConstants.DEATH)) {
            if (ch.getAnimationName().equals(AnimConstants.DEATH)) {
                ch.setAnim(AnimConstants.DEATH, 0f);
                ch.setLoopMode(LoopMode.Loop);
                ch.setSpeed(0f);
                setActionTime(getAttackChannel().getAnimMaxTime());
            }
            gameLogicCore.getRootNode().detachChild(this.getNode());
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
    public void update(float k) {
        if(dead)
            return;

        combatTracker.update(k);

        healthBarUpdate(k);

        damageIndicatorUpdate(k);

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
                animationDelegate.walkingAnimation();
                this.getPlayerStepsNode(this.isRunning()).play();
            } else if (this.getWalkDirection().length() == 0) {
                animationDelegate.idleAnimation();
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
                animationDelegate.stopAnimation();
                this.setBlocking(false);
            }
        } else if(this.isAttacking()) {
            if (this.getActionTime() <= 0 && !this.getAttackChannel().getAnimationName().equals(AnimConstants.ATTACK)) {
                this.attack();
            }
            if(!this.isAttack_pressed() && this.getActionTime() <= 0) {
                animationDelegate.stopAnimation();
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

    public void setPlayerDamaged() {
        playerDamaged = MAX_DAMAGED_TIME;
    }

    @Override
    public void setDamageIndicator(Picture damageIndicator) {
        this.damageIndicator = damageIndicator;
    }

    protected void damageIndicatorUpdate(float k) {
        if (playerDamaged > 0) {
            ((SimpleApplication) gameLogicCore.getApp()).getGuiNode().attachChild(gameLogicCore.getDamageIndicator());

            damageIndicator.getMaterial().setColor("Color",
                    new ColorRGBA(1f, 0f, 0f, .5f - (MAX_DAMAGED_TIME - playerDamaged) / (2*MAX_DAMAGED_TIME)));

            playerDamaged -= k;
        } else if (playerDamaged < 0) {
            ((SimpleApplication) gameLogicCore.getApp()).getGuiNode().detachChild(gameLogicCore.getDamageIndicator());
            playerDamaged = 0;
        }
    }

    protected void healthBarUpdate(float k) {
        healthBar.update();
    }

    @Override
    public void resetShootCounter() {
        shootDelay = shootRate;
    }

    @Override
    public void resetShootCounterByQuarter() {
        shootDelay = shootRate / 4;
    }

    @Override
    public void shootUpdate(float tpf) {
        if (shootDelay > 0f) {
            shootDelay -= tpf;
        }
    }

    @Override
    public boolean isReady() {
        return shootDelay < 0f;
    }

    @Override
    public void removeCharacter() {
    }

    @Override
    public void spawn() {
        gameLogicCore.getGameStateManager().setGameState(GameState.CALM);
        gameLogicCore.getGameStateManager().changeState(0.1f);

        gameLogicCore.detachGameOverIndicator();

        gameLogicCore.getRootNode().attachChild(this.getNode());

        if(!initializing) {
            healthBar.create();
        }

        dead = false;
    }

    @Override
    public void die() {
        dead = true;

        combatTracker.reset();

        gameLogicCore.getGameStateManager().setGameState(GameState.DEATH);

        animationDelegate.deathAnimation();

        this.getPlayerStepsNode(false).pause();

        gameLogicCore.attachGameOverIndicator();

        healthBar.destroy();
    }

    @Override
    public AnimationDelegateInterface getAnimationDelegate() {
        return animationDelegate;
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

    @Override
    public boolean isInitializing() {
        return initializing;
    }

    @Override
    public void setInitializing(boolean initializing) {
        this.initializing = initializing;
    }

    @Override
    public boolean isDead() {
        return dead;
    }

    public void setDead(boolean dead) {
        this.dead = dead;
    }

    public void setHealth(float health) {
        this.health = health;
    }

    public float getHealth() {
        return health;
    }
}
