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

import com.jme3.app.LegacyApplication;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;

import java.util.logging.Logger;

public class SoundManager {
    final private static Logger LOGGER = Logger.getLogger(SoundManager.class.getName());
    private AssetManager assetManager;
    private AudioNode playerStepsNode;

    public SoundManager(AssetManager assetManager, LegacyApplication app) {
        this.assetManager = assetManager;
        initialize();
    }

    private void initialize() {
        InitPlayerStepsNode();
    }

    private void InitPlayerStepsNode() {
        playerStepsNode = new AudioNode(assetManager, "Sounds/running.wav", AudioData.DataType.Buffer);
        playerStepsNode.setName("playerFootsteps");
        playerStepsNode.setLooping(true);
        playerStepsNode.setVolume(2);
        playerStepsNode.setPitch(0.65f);
    }

    public AudioNode getPlayerStepsNode(boolean running) {
        if (!running) {
            playerStepsNode.setPitch(0.65f);
        } else {
            playerStepsNode.setPitch(1.25f);
        }
        return playerStepsNode;
    }

    public void update(float tpf) {
        //TODO: play area/situation specific music here
    }
}
