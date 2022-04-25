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

import com.jme3.animation.SkeletonControl;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import ru.arifolth.game.CharacterInterface;
import ru.arifolth.game.Constants;

public class NonPlayerCharacter extends PlayerCharacter {
    protected float turnRate;
    protected float firingRange;
    protected float walkingRange;
    protected float walkSpeed;
    protected float shootRate;
    protected float shootDelay;
    protected Vector3f walkDirection, viewDirection;
    protected CharacterInterface playerCharacter;

    public NonPlayerCharacter() {
        this.setName(this.getClass().getName());

        this.turnRate = FastMath.QUARTER_PI / 5f;
        this.walkingRange = 500f;
        this.firingRange = 5f;
        this.walkSpeed = .3f;
        this.shootRate = 3.5f;
        this.shootDelay = 3f;
    }

    protected void initializeCharacterModel() {
        super.initializeCharacterModel();
        characterModel.rotate(0,3.14159f,0);
        SkeletonControl skeletonControl = characterModel.getControl(SkeletonControl.class);
        if(null != skeletonControl)
            skeletonControl.setHardwareSkinningPreferred(false);
    }

    public void setPlayerCharacter(CharacterInterface playerCharacter) {
        this.playerCharacter = playerCharacter;
    }

    public void update(float tpf) {
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
        if (shootDelay < 0f) {
            useWeapon();
            shootDelay = shootRate;
        }
    }

    public void shootUpdate(float tpf) {
        if (shootDelay > 0f) {
            shootDelay -= tpf;
        }
    }

    //TODO: Rewrite as a Melee/Ranged class later
    public void useWeapon() {
        System.out.println("ATTACK!");
        playSwordSound(getSwordSwingNode());

        if(!playerCharacter.isBlocking()) {
            playerCharacter.getHealthBar().setHealth(Constants.DAMAGE);
            System.out.println("HIT!");
            playSwordSound(getSwordHitNode());
        } else {
            System.out.println("BLOCKED!");
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
    }

    public void stop() {
        walkDirection.set(0f, 0f, 0f);
        characterControl.setWalkDirection(walkDirection);
        this.getPlayerStepsNode(this.isRunning()).pause();
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
    protected void initializeAnimation() { }

    @Override
    protected void initializeSkeletonDebug() { }

}
