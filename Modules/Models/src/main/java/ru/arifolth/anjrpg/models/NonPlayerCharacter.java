/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2022 Alexander Nilov
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
import com.jme3.animation.SkeletonControl;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import ru.arifolth.anjrpg.interfaces.CharacterInterface;
import ru.arifolth.anjrpg.interfaces.Constants;
import ru.arifolth.anjrpg.interfaces.Debug;

import java.util.logging.Level;
import java.util.logging.Logger;

public class NonPlayerCharacter extends PlayerCharacter {
    final private static Logger LOGGER = Logger.getLogger(NonPlayerCharacter.class.getName());
    protected float turnRate;
    protected float firingRange;
    protected float walkingRange;
    protected float walkSpeed;
    protected Vector3f walkDirection, viewDirection;
    protected CharacterInterface playerCharacter;

    public NonPlayerCharacter() {
        super();
        this.setName(this.getClass().getName());

        this.turnRate = FastMath.QUARTER_PI / 5f;
        this.walkingRange = 1000000f;
        this.firingRange = 5f;
        this.walkSpeed = .3f;
    }

    protected void initializeCharacterModel() {
        super.initializeCharacterModel();
        characterModel.rotate(0,3.14159f,0);
        SkeletonControl skeletonControl = characterModel.getControl(SkeletonControl.class);
        if(null != skeletonControl)
            skeletonControl.setHardwareSkinningPreferred(true);
    }

    public void setPlayerCharacter(CharacterInterface playerCharacter) {
        this.playerCharacter = playerCharacter;
    }

    public void update(float tpf) {
        if(isDead() || playerCharacter.isDead())
            return;

        shootUpdate(tpf);

        if (withinRange(walkingRange, playerCharacter)) {
            turningTo(playerCharacter.getCharacterControl().getPhysicsLocation());

            if (withinRange(firingRange, playerCharacter)) {
                stop();
                attack();
            } else {
                walkForward();
            }
        } else {
            stop();
        }

        healthBarUpdate(tpf);
    }

    protected void healthBarUpdate(float k) {
        healthBar.update();
    }

    public void jump() {
        characterControl.jump();
    }

    public void attack() {
        if (isReady()) {
            useWeapon();
            resetShootCounter();
        }
    }

    //TODO: Rewrite as a Melee/Ranged class later
    public void useWeapon() {
        animationDelegate.attackAnimation();
        LOGGER.log(Level.INFO, "NPC ATTACK!");
        playSwordSound(getSwordSwingNode());

        if(!playerCharacter.isBlocking()) {
            playerCharacter.getHealthBar().applyDamage(Constants.DAMAGE);
            LOGGER.log(Level.INFO, "HIT!");
            playSwordSound(getSwordHitNode());
        } else {
            LOGGER.log(Level.INFO, "BLOCKED!");
            playSwordSound(getSwordBlockNode());
        }
    }

    public void turningTo(Vector3f target) {
        Quaternion diff1 = new Quaternion();
        Quaternion diff2 = new Quaternion();

        Vector3f newOrient = target.subtract(characterControl.getPhysicsLocation());
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

    public void walkForward() {
        walkDirection = viewDirection = characterControl.getViewDirection().normalize().mult(walkSpeed);
        characterControl.setWalkDirection(walkDirection);
        this.getPlayerStepsNode(this.isRunning()).play();
        animationDelegate.walkingAnimation();
    }

    public void stop() {
        if(walkDirection != null) {
            walkDirection.set(0f, 0f, 0f);
            characterControl.setWalkDirection(walkDirection);
        }
        this.getPlayerStepsNode(this.isRunning()).pause();
        animationDelegate.idleAnimation();
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
    protected void initializeSkeletonDebug() {
        AppSettings settings = gameLogicCore.getApp().getContext().getSettings();
        boolean debug = settings.getBoolean(Constants.DEBUG);
        if(debug) {
            Debug.showNodeAxes(gameLogicCore.getAssetManager(), this.getNode(), 5);
        }
    }

    @Override
    public void spawn() {
        Node enemies = gameLogicCore.getEnemies();
        enemies.attachChild(this.getNode());
    }

    @Override
    public void onAnimCycleDone(AnimControl ctrl, AnimChannel ch, String name) {
        if (isDead()) {
            removeCharacter();
        } else if(name.equals(AnimConstants.BLOCK) && isReady()) {
            if (!ch.getAnimationName().equals(AnimConstants.IDLE)) {
                ch.setAnim(AnimConstants.IDLE, 0f);
                ch.setLoopMode(LoopMode.Loop);
                ch.setSpeed(1f);
            }
        }
    }

    @Override
    public void removeCharacter() {
        this.getNode().removeControl(characterControl);
        this.getNode().detachChild(characterModel);

        this.getPlayerStepsNode(false).pause();

        gameLogicCore.getRootNode().detachChild(this.getNode());
        gameLogicCore.getCharacterMap().remove(this.getNode());
        this.getHealthBar().destroy();

        removePhysixControl();
    }

    @Override
    public void die() {
        animationDelegate.deathAnimation();

        setDead(true);
    }
}
