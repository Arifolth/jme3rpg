/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2014 - 2024 Alexander Nilov
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

package ru.arifolth.anjrpg.interfaces;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;

public enum SoundTypeEnum implements AudioType {
    WIND {
        private AudioData.DataType stream = AudioData.DataType.Stream;
        private Float pitch = null;
        private boolean positional = false;
        private boolean looping = true;

        @Override
        public void init() {
            createAudioNode(this,
                    "Sounds/birds/459977__florianreichelt__soft-wind.ogg",
                    stream,
                    looping,
                    positional,
                    pitch, volumeMultiplier);
        }

    },
    WEATHER {
        @Override
        public void init() {

        }
    },
    FOOTSTEPS {
        private AudioData.DataType buffer = AudioData.DataType.Buffer;
        private float pitch = 0.65f;
        private Boolean positional = null;
        private boolean looping = true;

        @Override
        public void init() {
            createAudioNode(this,
                    "Sounds/running.wav",
                    buffer,
                    looping,
                    positional,
                    pitch
            );
        }

        public static void createAudioNode(AudioType audioType, String name, AudioData.DataType dataType, Boolean looping, Boolean positional, Float pitch) {
            AudioNode audioNode = new AudioNode(assetManager, name, dataType);
            audioNode.setVolume(Constants.SOUND_VOLUME / 4);
            audioNode.setMaxDistance(15);

            if(looping != null)
                audioNode.setLooping(looping);
            if(positional != null)
                audioNode.setPositional(positional);
            if(pitch != null)
                audioNode.setPitch(pitch);

            soundMap.put(audioType, audioNode);
        }
    },
    SWORD_SWING {

        private AudioData.DataType buffer = AudioData.DataType.Buffer;
        private float pitch = Constants.SOUND_PITCH;
        private Boolean positional = null;
        private Boolean looping = null;

        @Override
        public void init() {
            {
                createAudioNode(this,
                        "Sounds/swing/209121__lukesharples__sword-swipe11.wav",
                        buffer,
                        looping,
                        positional,
                        pitch,
                        volumeMultiplier);
            }
            {
                createAudioNode(this,
                        "Sounds/swing/209122__lukesharples__sword-swipe2.wav",
                        buffer,
                        looping,
                        positional,
                        pitch,
                        volumeMultiplier);
            }
            {
                createAudioNode(this,
                        "Sounds/swing/209123__lukesharples__sword-swipe13.wav",
                        buffer,
                        looping,
                        positional,
                        pitch,
                        volumeMultiplier);
            }
            {
                createAudioNode(this,
                        "Sounds/swing/209124__lukesharples__sword-swipe4.wav",
                        buffer,
                        looping,
                        positional,
                        pitch,
                        volumeMultiplier);
            }
            {
                createAudioNode(this,
                        "Sounds/swing/209125__lukesharples__sword-swipe3.wav",
                        buffer,
                        looping,
                        positional,
                        pitch,
                        volumeMultiplier);
            }
            {
                createAudioNode(this,
                        "Sounds/swing/209126__lukesharples__sword-swipe6.wav",
                        buffer,
                        looping,
                        positional,
                        pitch,
                        volumeMultiplier);
            }
            {
                createAudioNode(this,
                        "Sounds/swing/209127__lukesharples__sword-swipe5.wav",
                        buffer,
                        looping,
                        positional,
                        pitch,
                        volumeMultiplier
                );
            }
        }
    },
    SWORD_HIT {
        private AudioData.DataType buffer = AudioData.DataType.Buffer;
        private float pitch = Constants.SOUND_PITCH;
        private Boolean positional = null;
        private Boolean looping = null;

        @Override
        public void init() {
            {
                createAudioNode(this,
                        "Sounds/hit/215008__taira-komori__stabbing.ogg",
                        buffer,
                        looping,
                        positional,
                        pitch,
                        volumeMultiplier);
            }
        }
    },
    MENU {
        private AudioData.DataType buffer = AudioData.DataType.Buffer;
        private Float pitch = null;
        private boolean positional = false;
        private Boolean looping = null;

        @Override
        public void init() {
            createAudioNode(this,
                    "Sounds/menu/71469__natharra__blink-4-damped.ogg",
                    buffer,
                    looping,
                    positional,
                    pitch,
                    volumeMultiplier);
        }
    },
    SWORD_BLOCK {
        private AudioData.DataType buffer = AudioData.DataType.Buffer;
        private float pitch = Constants.SOUND_PITCH;
        private Boolean positional = null;
        private Boolean looping = null;

        @Override
        public void init() {
            {
                createAudioNode(this,
                        "Sounds/block/213696__taira-komori__sword1.ogg",
                        buffer,
                        looping,
                        positional,
                        pitch,
                        volumeMultiplier);
            }
            {
                createAudioNode(this,
                        "Sounds/block/213695__taira-komori__sword2.ogg",
                        buffer,
                        looping,
                        positional,
                        pitch,
                        volumeMultiplier);
            }
            {
                createAudioNode(this,
                        "Sounds/block/213694__taira-komori__sword3.ogg",
                        buffer,
                        looping,
                        positional,
                        pitch,
                        volumeMultiplier);
            }
        }
    };

    private static AssetManager assetManager;
    private static float volumeMultiplier = 2;


    public static void setAssetManager(AssetManager assetManager) {
        SoundTypeEnum.assetManager = assetManager;
    }

    public static AudioNode getClone(AudioType audioType) {
        return Utils.getRandomObject(soundMap.get(audioType)).clone();
    }

    public static AudioNode getOriginal(AudioType audioType) {
        return Utils.getSingleObject(soundMap.get(audioType));
    }

    public static void createAudioNode(AudioType audioType, String name, AudioData.DataType dataType, Boolean looping, Boolean positional, Float pitch, Float volumeMultiplier) {
        AudioNode audioNode = new AudioNode(assetManager, name, dataType);
        float soundVolume = volumeMultiplier != null ? Constants.SOUND_VOLUME * volumeMultiplier : Constants.SOUND_VOLUME;
        audioNode.setVolume(soundVolume);

        if(looping != null)
            audioNode.setLooping(looping);
        if(positional != null)
            audioNode.setPositional(positional);
        if(pitch != null)
            audioNode.setPitch(pitch);

        soundMap.put(audioType, audioNode);
    }

    public static void reInitialize(float volume) {
        soundMap.values().forEach(audioNode -> audioNode.setVolume(volume));
    }
    private static final Multimap<AudioType, AudioNode> soundMap = ArrayListMultimap.create();
}
