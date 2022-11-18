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

import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import ru.arifolth.anjrpg.interfaces.CharacterInterface;
import ru.arifolth.anjrpg.interfaces.GameLogicCoreInterface;
import ru.arifolth.anjrpg.interfaces.HealthBarInterface;

public abstract class BaseCharacter implements CharacterInterface {
    protected GameLogicCoreInterface gameLogicCore;
    protected CharacterControl characterControl;
    protected Spatial characterModel;
    protected HealthBarInterface healthBar;
    private Node characterNode = new Node();

    public BaseCharacter() {
    }

    @Override
    public Spatial getCharacterModel() {
        return characterModel;
    }

    @Override
    public CharacterControl getCharacterControl() {
        return characterControl;
    }

    @Override
    public Node getNode() {
        return characterNode;
    }

    protected void setUpCharacterNode() {
        characterNode.addControl(characterControl);
        characterNode.attachChild(characterModel);
    }

    protected abstract void initializeCharacterModel();

    protected void initializePhysixControl() {
        // We set up collision detection for the characterControl by creating
        // a capsule collision shape and a CharacterControl.
        // The CharacterControl offers extra settings for
        // size, stepheight, jumping, falling, and gravity.
        // We also put the characterControl in its starting position.
        CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(1.5f, 6f, 1);
        characterControl = new CharacterControl(capsuleShape, 0.8f);
        setUpDefaultPhysics();
        addPhysixControl();
    }

    private void addPhysixControl() {
        gameLogicCore.getBulletAppState().getPhysicsSpace().add(characterControl);
    }

    protected void removePhysixControl() {
        gameLogicCore.getBulletAppState().getPhysicsSpace().remove(characterControl);
    }

    protected void setUpDefaultPhysics() {
        characterControl.setJumpSpeed(0);
        characterControl.setFallSpeed(0);
        characterControl.setGravity(0);
    }

    protected abstract void initHealthBar();

    public String getName() {
        return getNode().getName();
    }

    public void setName(String name) {
        getNode().setName(name);
    }

    public void initialize(GameLogicCoreInterface gameLogicCore) {
        this.gameLogicCore = gameLogicCore;

        initializePhysixControl();

        initializeCharacterModel();

        setUpCharacterNode();

        initHealthBar();

        initializeSounds();

        initializeAnimation();

        initializeSkeletonDebug();
    }

    public boolean withinRange(float distance, CharacterInterface enemy) {
        if(enemy.isDead())
            return false;

        CharacterControl control = enemy.getCharacterControl();
        float dist = control.getPhysicsLocation().distanceSquared(characterControl.getPhysicsLocation());

        if (dist <= distance * distance) {
            return true;
        }

        return false;
    }

    public abstract void initializeSounds();
    protected abstract void initializeAnimation();
    protected abstract void initializeSkeletonDebug();

    @Override
    public HealthBarInterface getHealthBar() {
        return healthBar;
    }
}
