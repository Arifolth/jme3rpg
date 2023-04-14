/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2014 - 2023 Alexander Nilov
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

package ru.arifolth.sound;

import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioSource;
import ru.arifolth.anjrpg.interfaces.*;

import java.util.EnumSet;
import java.util.logging.Logger;

public class SoundManager implements SoundManagerInterface {
    final private static Logger LOGGER = Logger.getLogger(SoundManager.class.getName());
    private float soundVolume = Constants.SOUND_VOLUME;
    private float musicVolume = Constants.SOUND_VOLUME / Constants.MUSIC_VOLUME_MULTIPLIER;
    private AudioNode currentMusicNode;
    private MusicType nextMusicType;
    private float fadeOut;

    @Override
    public AudioNode getCurrentMusicNode() {
        return currentMusicNode;
    }

    @Override
    public void fadeMusicOut(float tpf, MusicType nextMusicType) {
        if(fadeOut == 0) {
            fadeOut = tpf / 2;
        }
        this.nextMusicType = nextMusicType;
    }

    public SoundManager(AssetManager assetManager) {
        SoundType.setAssetManager(assetManager);
        MusicType.setAssetManager(assetManager);

        initialize();
    }

    @Override
    public void initialize() {
        EnumSet.allOf(SoundType.class).forEach(SoundType::init);
        EnumSet.allOf(MusicType.class).forEach(MusicType::init);
    }

    public AudioNode getSoundNode(AudioType musicType) {
        return SoundType.getClone(musicType);
    }

    public AudioNode getMusicNode(AudioType musicType) {
        return MusicType.getOriginal(musicType);
    }

    @Override
    public AudioNode getMenuNode() {
        return SoundType.getOriginal(SoundType.MENU);
    }

    @Override
    public void update(float tpf) {
        if((fadeOut > 0) && (currentMusicNode.getVolume() > 0)) {
            float volume = currentMusicNode.getVolume() - fadeOut;
            currentMusicNode.setVolume(Math.max(volume, 0f));
        } else if(nextMusicType != null){
            fadeOut = 0;
            currentMusicNode = getMusicNode(nextMusicType);
            nextMusicType = null;
            currentMusicNode.setVolume(soundVolume);
            currentMusicNode.stop();
            currentMusicNode.play();
        }
    }

    @Override
    public void setNextMusicType(MusicType nextMusicType) {
        this.nextMusicType = nextMusicType;
    }

    public void setVolume(float volume) {
        this.soundVolume = volume;
        this.musicVolume = soundVolume / Constants.MUSIC_VOLUME_MULTIPLIER;
    }

    public void reInitialize(GameLogicCoreInterface gameLogicCore) {
        {
            //iterate over sound nodes. For now we have only Foot Steps
            String soundName = SoundType.FOOTSTEPS.name();
            ((AudioNode) gameLogicCore.getPlayerCharacter().getNode().getChild(soundName)).setVolume(soundVolume);
            gameLogicCore.getCharacterMap().values().forEach(
                character -> ((AudioNode) character.getNode().getChild(soundName)).setVolume(soundVolume)
            );

            SoundType.reInitialize(soundVolume);
        }
        {
            currentMusicNode.setVolume(musicVolume);
            MusicType.reInitialize(musicVolume);
        }
    }
}
