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

package ru.arifolth.game;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

public interface CharacterInterface {
    void simpleUpdate(float tpf);
    Spatial getCharacterModel();
    CharacterControl getCharacterControl();
    Node getNode();

    void initializeSounds();

    void setCam(Camera cam);

    void setLeft(boolean pressed);

    void setRight(boolean pressed);

    void setUp(boolean pressed);

    void setDown(boolean pressed);

    void setJump_pressed(boolean b);

    void setRunning(boolean pressed);

    boolean isCapture_mouse();

    boolean isJumping();

    void setBlock_pressed(boolean pressed);

    boolean isBlock_pressed();

    void setBlocking(boolean b);

    void setAttacking(boolean b);

    boolean isAttack_pressed();

    void setAttack_pressed(boolean pressed);

    void initialize(BulletAppState bulletAppState, AssetManager assetManager, SoundManagerInterface soundManager);

    boolean isAttacking();

    boolean isBlocking();

    HealthBarInterface getHealthBar();
}
