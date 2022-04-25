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
import com.jme3.input.FlyByCamera;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;

import java.util.List;
import java.util.Set;

public interface GameLogicCoreInterface {

    CharacterInterface getPlayerCharacter();

    void reInitialize();

    List<DamageControlInterface> getDamageSet();

    List<CharacterInterface> getCharacterSet();

    MovementControllerInterface getMovementController();

    void initialize();

    void update(float tpf);

    BulletAppState getBulletAppState();

    Node getRootNode();

    AssetManager getAssetManager();

    SoundManagerInterface getSoundManager();

    Camera getCam();

    FlyByCamera getFlyCam();
}
