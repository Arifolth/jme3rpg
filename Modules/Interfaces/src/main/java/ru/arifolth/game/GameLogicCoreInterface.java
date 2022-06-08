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

import com.jme3.app.Application;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.input.FlyByCamera;
import com.jme3.input.InputManager;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.ui.Picture;
import ru.arifolth.anjrpg.weather.Emitter;

import java.util.Map;
import java.util.Set;

public interface GameLogicCoreInterface {

    CharacterInterface getPlayerCharacter();

    void attachPlayer();

    void detachNPCs();

    void attachInitialNPCs();

    void initPlayerComplete();

    void initNPCsComplete();

    void attachNPCs();

    void reInitialize();

    Map<Node,CharacterInterface> getCharacterMap();

    MovementControllerInterface getMovementController();

    void initialize();

    Picture getDamageIndicator();

    void setDamageIndicator(Picture damageIndicator);

    void positionNPCs(Map<Node, CharacterInterface> characterMap);

    void update(float tpf);

    Set<Emitter> getWeatherEffectsSet();

    BulletAppState getBulletAppState();

    Node getRootNode();

    AssetManager getAssetManager();

    SoundManagerInterface getSoundManager();

    Camera getCam();

    FlyByCamera getFlyCam();

    Application getApp();

    InputManager getInputManager();

    Node getEnemies();

    void attachGameOverIndicator();

    void detachGameOverIndicator();

    void setupPlayer();

    void setupNPCs();

    void setupCamera();

    void enablePlayerPhysics();

    void enableNPCsPhysics();

    void positionPlayer();
}
