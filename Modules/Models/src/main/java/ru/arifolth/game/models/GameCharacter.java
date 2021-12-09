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

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import ru.arifolth.game.CharacterInterface;
import ru.arifolth.game.SoundManager;
import ru.arifolth.game.SoundManagerInterface;

public abstract class GameCharacter implements CharacterInterface {
    protected BulletAppState bulletAppState;
    protected AssetManager assetManager;

    protected CharacterControl characterControl;
    protected Spatial characterModel;
    protected SoundManagerInterface soundManager;
    private Node characterNode;

    public GameCharacter() {
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

    protected void initializeCharacterNode() {
        characterNode = new Node("Player");
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
        bulletAppState.getPhysicsSpace().add(characterControl);
    }

    private void setUpDefaultPhysics() {
        characterControl.setJumpSpeed(0);
        characterControl.setFallSpeed(0);
        characterControl.setGravity(0);
    }

    protected abstract void initializeHealthBar();

    public void initialize(BulletAppState bulletAppState, AssetManager assetManager, SoundManagerInterface soundManager) {
        this.bulletAppState = bulletAppState;
        this.assetManager = assetManager;
        this.soundManager = soundManager;

        initializePhysixControl();

        initializeCharacterModel();

        initializeCharacterNode();

        initializeHealthBar();

        initializeSounds();

        initializeAnimation();

        initializeSkeletonDebug();
    }

    public abstract void initializeSounds();
    protected abstract void initializeAnimation();
    protected abstract void initializeSkeletonDebug();

    public abstract boolean isAttacking();
    public abstract boolean isBlocking();

}
