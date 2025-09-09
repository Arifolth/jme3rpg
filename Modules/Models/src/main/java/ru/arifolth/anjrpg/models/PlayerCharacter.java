/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2014 - 2025 Alexander Nilov
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
import com.jme3.math.*;
import com.jme3.renderer.Camera;
import com.jme3.ui.Picture;
import ru.arifolth.anjrpg.interfaces.*;
import ru.arifolth.anjrpg.interfaces.bars.ManaBarInterface;
import ru.arifolth.anjrpg.interfaces.bars.StaminaBarInterface;
import ru.arifolth.anjrpg.interfaces.camera.DeathAwareCameraInterface;
import ru.arifolth.anjrpg.models.bars.PlayerHealthBar;
import ru.arifolth.anjrpg.models.bars.PlayerManaBar;
import ru.arifolth.anjrpg.models.bars.PlayerStaminaBar;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PlayerCharacter extends AnimatedCharacter {
    final private static Logger LOGGER = Logger.getLogger(PlayerCharacter.class.getName());

    public static final String PLAYER_CHARACTER_MODEL = "Models/Ninja/Ninja.j3o";
    protected final AnimationDelegateInterface animationDelegate = new AnimationDelegate(this);
    private final float walkingRange;
    protected float shootDelay;
    protected float shootRate;
    protected float turnRate;
    protected Vector3f viewDirection;

    private boolean left = false, right = false, up = false, down = false,
        attacking = false, capture_mouse = true, running = false, blocking = false, block_pressed = false,
        jumping = false, jump_pressed = false, attack_pressed = false;
    private Vector3f walkDirection = Constants.ZERO_VECTOR3F;
    private float airTime = 0;
    private float actionTime = 0;
    private Camera cam;
    private static final float MAX_DAMAGED_TIME = Constants.SHOOT_DELAY;
    private float playerDamaged = 0f;
    private float jumpCooldown = 0f;
    private Picture damageIndicator;
    protected float firingRange;
    protected boolean dead = false;
    protected boolean initializing = true;
    private float health;

    private Map.Entry<Iterator<CharacterInterface>, CharacterInterface> lockedOnCharacter = null;
    private final Ray attackRay = new Ray();
    public PlayerCharacter() {
        this.setModel(PLAYER_CHARACTER_MODEL);
        this.setName(this.getClass().getName());
        this.walkingRange = Constants.WALKING_RANGE;
        this.firingRange = Constants.MELEE_DISTANCE_LIMIT;
        this.shootDelay = Constants.SHOOT_DELAY;
        this.shootRate = Constants.SHOOT_RATE;

        this.turnRate = Constants.TURN_RATE;
    }

    @Override
    public void initializeModelLod() {
        //NO LODs FOR THE MODELS, TOO AGGRESSIVE
    }

    @Override
    protected void initBars() {
        healthBar = new PlayerHealthBar(gameLogicCore.getAssetManager(), this, (SimpleApplication) gameLogicCore.getApp());
        healthBar.init();

        manaBar = new PlayerManaBar(gameLogicCore.getAssetManager(), this, (SimpleApplication) gameLogicCore.getApp());
        manaBar.init();

        staminaBar = new PlayerStaminaBar(gameLogicCore.getAssetManager(), this, (SimpleApplication) gameLogicCore.getApp());
        staminaBar.init();
    }

    public void setCam(Camera cam) {
        this.cam = cam;
    }

    public void block() {
        animationDelegate.blockAnimation();

        playSound(getSwordSwingNode());
    }

    public void attack() {
        animationDelegate.attackAnimation();
        playSound(getSwordSwingNode());

        Map.Entry<Iterator<CharacterInterface>, CharacterInterface> lock = getLockedOnCharacter();
        if(lock != null && !lock.getValue().isDead()) {
            Vector3f playerForward = characterControl.getViewDirection().negate().normalize();
            Vector3f playerPos = characterControl.getPhysicsLocation().clone();
            CharacterInterface npc = lock.getValue();
            Vector3f npcPos = npc.getNode().getWorldTranslation();

            // Distance check before collision test
            float distance = characterControl.getPhysicsLocation().distance(npcPos);
            if (distance > Constants.MELEE_DISTANCE_LIMIT * 1.5f)
                return;

            Vector3f toNpc = npcPos.subtract(playerPos).normalize();
            float angleBetween = playerForward.angleBetween(toNpc) * FastMath.RAD_TO_DEG;

            if (angleBetween <= Constants.CONE_DEGREES / 2.0f) {
                boolean blocked = Utils.getRandom(Constants.HIT_PROBABILITY);
                if(!blocked) {
                    npc.getHealthBar().applyDamage(Constants.DAMAGE);
                    playSound(getSwordHitNode());
                } else {
                    npc.getAnimationDelegate().blockAnimation();
                    npc.resetShootCounterByQuarter();
                    playSound(getSwordBlockNode());
                }
            }
        } else {
            // No locked target - area attack
            Vector3f playerForward = characterControl.getViewDirection().negate().normalize();
            Vector3f playerPos = characterControl.getPhysicsLocation().clone();

            Iterator<CharacterInterface> iterator = gameLogicCore.getCharacterMap().values().iterator();
            while (iterator.hasNext()) {
                CharacterInterface npc = iterator.next();

                // Skip dead characters and the player itself
                if (npc.isDead() || npc == this) {
                    continue;
                }

                Vector3f characterPos = npc.getNode().getWorldTranslation();

                // Distance check - must be reachable
                float distance = playerPos.distance(characterPos);
                if (distance > Constants.MELEE_DISTANCE_LIMIT * 1.5f) {
                    continue;
                }

                // Angle check - must be within 45-degree cone (22.5 degrees on each side)
                Vector3f toCharacter = characterPos.subtract(playerPos).normalize();
                float angleBetween = playerForward.angleBetween(toCharacter) * FastMath.RAD_TO_DEG;

                if (angleBetween <= Constants.CONE_DEGREES / 2.0f) {
                    // NPC is within attack cone and range
                    boolean blocked = Utils.getRandom(Constants.HIT_PROBABILITY);
                    if(!blocked) {
                        npc.getHealthBar().applyDamage(Constants.DAMAGE);
                        playSound(getSwordHitNode());
                    } else {
                        npc.getAnimationDelegate().blockAnimation();
                        npc.resetShootCounterByQuarter();
                        playSound(getSwordBlockNode());
                    }
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

            gameLogicCore.getApp().enqueue(() -> {
                gameLogicCore.getRootNode().detachChild(this.getNode());
            });
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

        barsUpdate(k);

        damageIndicatorUpdate(k);

        movementUpdate(k);
    }

    public void stop() {
        if(walkDirection != null) {
            walkDirection.set(0f, 0f, 0f);
            characterControl.setWalkDirection(walkDirection);
        }
        this.getPlayerStepsNode(this.isRunning()).pause();
        animationDelegate.idleAnimation();
    }

    public void movementUpdate(float k) {
        if (dead)
            return; // Early exit for dead characters

        float movement_amount = 0.3f;

        if (this.isRunning()) {
            // Consume stamina when running
            staminaBar.consumeStamina(Constants.STAMINA_CONSUMPTION_RUN * k);
            if(!staminaBar.isExhausted()) {
                movement_amount *= 1.75;
            } else {
                setRunning(false);
            }
        }

        // Gets forward direction and moves it forward
        Vector3f camDir = this.getCam().getDirection().clone().multLocal(movement_amount);
        // Gets left direction and moves it to the left
        Vector3f camLeft = this.getCam().getLeft().clone().multLocal(movement_amount * 0.75f);

        // We don't want to fly or go underground
        camDir.y = 0;
        camLeft.y = 0;

        this.getWalkDirection().set(0, 0, 0); // The walk direction is initially null

        if (lockedOnCharacter != null) {
            CharacterInterface targetCharacter = lockedOnCharacter.getValue();
            if(withinRange(walkingRange, targetCharacter)) {
                turningTo(targetCharacter.getCharacterControl().getPhysicsLocation());

                if (withinRange(firingRange, targetCharacter)) {
                    stop();
                    this.getWalkDirection().set(walkDirection.getX(), 0, 0);
                 }
            } else {
                lockedOnCharacter = null;
            }
        }


        if (this.isUp()) {
            this.getWalkDirection().addLocal(camDir);

            if (this.isLeft()) {
                this.getWalkDirection().addLocal(camLeft);
            } else if (this.isRight()) {
                this.getWalkDirection().addLocal(camLeft.negate());
            }
        } else if (this.isDown()) {
            this.getWalkDirection().addLocal(camDir.negate());

            if (this.isLeft()) {
                this.getWalkDirection().addLocal(camLeft);
            } else if (this.isRight()) {
                this.getWalkDirection().addLocal(camLeft.negate());
            }
        } else if (this.isLeft()) {
            this.getWalkDirection().addLocal(camLeft);
        } else if (this.isRight()) {
            this.getWalkDirection().addLocal(camLeft.negate());
        }

        // Turn character to face movement direction when moving left/right
        if (lockedOnCharacter == null) {
            if (this.isLeft() && !this.isUp() && !this.isDown()) {
                // Pure left movement - turn to face left direction
                Vector3f leftDir = this.getCam().getLeft().clone();
                leftDir.y = 0;
                leftDir.normalizeLocal();
                characterControl.setViewDirection(leftDir);
            } else if (this.isRight() && !this.isUp() && !this.isDown()) {
                // Pure right movement - turn to face right direction
                Vector3f rightDir = this.getCam().getLeft().clone().negate();
                rightDir.y = 0;
                rightDir.normalizeLocal();
                characterControl.setViewDirection(rightDir);
            }
        }

        if(this.isJumping()) {
            LOGGER.log(Level.INFO, "JUMP PRESSED");

            if (this.getCharacterControl().onGround() && !this.getAnimationChannel().getAnimationName().equals(AnimConstants.JUMP)) {
                LOGGER.log(Level.INFO, "JUMP ANIMATION");

                this.getAttackChannel().setAnim("JumpNoHeight");
                this.getAttackChannel().setSpeed(1f);
                this.getAttackChannel().setLoopMode(LoopMode.DontLoop);

                this.getAnimationChannel().setAnim(AnimConstants.JUMP);
                this.getAnimationChannel().setSpeed(1f);
                this.getAnimationChannel().setLoopMode(LoopMode.DontLoop);
            }

            if (this.getAnimationChannel().getAnimationName().equals(AnimConstants.JUMP)) {
                LOGGER.log(Level.INFO, "JUMPING");
                staminaBar.consumeStamina(Constants.STAMINA_CONSUMPTION_JUMP);
                characterControl.getControllerId().setJumpSpeed(15f);
                this.getCharacterControl().jump();
            }

            if(this.getAirTime() > 1f ) {
                LOGGER.log(Level.INFO, "GROUNDING");

                this.setJumping(false);
                characterControl.getControllerId().setJumpSpeed(-1f);
                characterControl.setWalkDirection(Constants.RAY_DOWN);
            } else {
                this.setAirTime(this.getAirTime() + k);

                LOGGER.log(Level.INFO, "IN AIR: " + k);
            }
        }

        if(this.getCharacterControl().onGround() && jumpCooldown > 0)
            jumpCooldown -= k;

        if (!this.isJumping() && this.getCharacterControl().onGround()) {
            if(this.getAirTime() > 0) {
                this.setAirTime(0);
                jumpCooldown = Constants.JUMP_COOLDOWN;
                playSound(getJumpNode());
            }

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

        if (this.getActionTime() > 0) {
            this.setActionTime(this.getActionTime() - k);
        }

        if (this.isBlocking()) {
            if (this.getActionTime() <= 0 && !this.getAttackChannel().getAnimationName().equals(AnimConstants.BLOCK)) {
                this.block();
            }
            if (!this.isBlock_pressed() && this.getActionTime() <= 0) {
                animationDelegate.stopAnimation();
                this.setBlocking(false);
            }
        } else if (this.isAttacking()) {
            if (this.getActionTime() <= 0 && !this.getAttackChannel().getAnimationName().equals(AnimConstants.ATTACK)) {
                this.attack();
            }
            if (!this.isAttack_pressed() && this.getActionTime() <= 0) {
                animationDelegate.stopAnimation();
                this.setAttacking(false);
            }
        }

        characterControl.setWalkDirection(this.getWalkDirection());

        if (lockedOnCharacter == null) {
            // Rotate model to point walk direction if moving
            if ((this.getWalkDirection().length() != 0) && (this.isUp() || this.isLeft() || this.isRight()))
                characterControl.setViewDirection(this.getWalkDirection().negate());

            //walk backwards
            if ((this.getWalkDirection().length() != 0) && this.isDown())
                characterControl.setViewDirection(this.getWalkDirection());
        }
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

    protected void barsUpdate(float k) {
        if (initializing)
            return;

        healthBar.update();
        manaBar.update();
        staminaBar.update();
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

    public void turningTo(Vector3f target) {
        Quaternion diff1 = new Quaternion();
        Quaternion diff2 = new Quaternion();

        Vector3f newOrient = target.subtract(characterControl.getPhysicsLocation()).negate();
        newOrient.setY(0f);
        Vector3f curOrient = characterControl.getViewDirection();

        diff1.lookAt(newOrient, Vector3f.UNIT_Y);
        diff2.lookAt(curOrient, Vector3f.UNIT_Y);
        Quaternion diff3 = diff1.subtract(diff2);

        float ydiff = diff3.getY();

        if (FastMath.abs(ydiff) > turnRate) {
            if (ydiff < 0) {
                turnRight();
            } else {
                turnLeft();
            }
        } else {
            characterControl.setViewDirection(newOrient);
        }
    }

    public void turnLeft() {
        viewDirection = characterControl.getViewDirection();
        Vector3f temp = viewDirection.normalize();
        Quaternion turn = new Quaternion();
        turn.fromAngleAxis(turnRate, Vector3f.UNIT_Y);
        temp = turn.mult(temp);
        characterControl.setViewDirection(temp);
    }

    public void turnRight() {
        viewDirection = characterControl.getViewDirection();
        Vector3f temp = viewDirection.normalize();
        Quaternion turn = new Quaternion();
        turn.fromAngleAxis(-turnRate, Vector3f.UNIT_Y);
        temp = turn.mult(temp);
        characterControl.setViewDirection(temp);
    }

    @Override
    public void spawn() {
        gameLogicCore.getGameStateManager().setGameState(GameState.CALM);

        gameLogicCore.detachGameOverIndicator();

        // Reset camera to normal mode before respawning
        DeathAwareCameraInterface camera = (DeathAwareCameraInterface) gameLogicCore.getFreeFollowCamera();
        camera.deactivateDeathCamera();

        gameLogicCore.getApp().enqueue(() -> {
            gameLogicCore.getRootNode().attachChild(this.getNode());
        });

        healthBar.create();
        manaBar.create();
        staminaBar.create();

        dead = false;
    }

    @Override
    public void die() {
        dead = true;
        combatTracker.reset();
        gameLogicCore.getGameStateManager().setGameState(GameState.DEATH);

        Vector3f deathPosition = characterControl.getPhysicsLocation().clone();

        DeathAwareCameraInterface camera = (DeathAwareCameraInterface) gameLogicCore.getFreeFollowCamera();
        camera.activateDeathCamera(deathPosition);

        animationDelegate.deathAnimation();

        this.getPlayerStepsNode(false).pause();
        gameLogicCore.attachGameOverIndicator();

        // Destroy all bars
        healthBar.destroy();
        manaBar.destroy();
        staminaBar.destroy();

        lockedOnCharacter = null;
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
        this.jump_pressed = ((jumpCooldown <= 0) && jump_pressed);
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

    @Override
    public ManaBarInterface getManaBar() {
        return manaBar;
    }

    @Override
    public StaminaBarInterface getStaminaBar() {
        return staminaBar;
    }

    @Override
    public void lockOnTarget() {
        if(lockedOnCharacter == null) {  //start all over
            Iterator<CharacterInterface> iterator = gameLogicCore.getCharacterMap().values().iterator();
            while (iterator.hasNext()) {
                CharacterInterface character = iterator.next();
                if (withinRange(walkingRange, character)) {
                    lockedOnCharacter = new AbstractMap.SimpleEntry<>(iterator,character);
                    return;
                }
            }
        } else {
            Iterator<CharacterInterface> iterator = lockedOnCharacter.getKey();
            if(!iterator.hasNext()) {
                lockedOnCharacter = null;
            }
            while(iterator.hasNext()) {
                CharacterInterface character = iterator.next();
                if (withinRange(walkingRange, character)) {
                    lockedOnCharacter = new AbstractMap.SimpleEntry<>(iterator,character);
                    return; //fast exit
                }
            }
        }
    }

    @Override
    public Map.Entry<Iterator<CharacterInterface>, CharacterInterface> getLockedOnCharacter() {
        return lockedOnCharacter;
    }
}
