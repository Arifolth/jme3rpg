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

package ru.arifolth.game;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;

import java.util.EnumSet;
import java.util.logging.Logger;

import static ru.arifolth.game.SoundManager.SoundType.getClone;

public class SoundManager implements SoundManagerInterface {
    final private static Logger LOGGER = Logger.getLogger(SoundManager.class.getName());

    private static final Multimap<SoundType, AudioNode> soundMap = ArrayListMultimap.create();
    private static AssetManager assetManager;

    private static float volume = Constants.SOUND_VOLUME;
    private static final float pitch = Constants.SOUND_PITCH;

    enum SoundType {
        WIND {
            @Override
            void init() {
                AudioNode audioNode = new AudioNode(assetManager, "Sounds/birds/459977__florianreichelt__soft-wind.ogg", AudioData.DataType.Stream);
                audioNode.setLooping(true);
                audioNode.setPositional(false);

                soundMap.put(SoundType.WIND, audioNode);
            }
        },
        WEATHER {
            @Override
            void init() {

            }
        },
        FOOTSTEPS {
            @Override
            void init() {
                AudioNode audioNode = new AudioNode(assetManager, "Sounds/running.wav", AudioData.DataType.Buffer);
                audioNode.setLooping(true);
                audioNode.setVolume(volume);
                audioNode.setPitch(0.65f);

                soundMap.put(SoundType.FOOTSTEPS, audioNode);
            }
        },
        SWORD_SWING {
            @Override
            void init() {
                {
                    AudioNode audioNode = new AudioNode(assetManager, "Sounds/swing/209121__lukesharples__sword-swipe11.wav", AudioData.DataType.Buffer);
                    audioNode.setVolume(volume);
                    audioNode.setPitch(pitch);
                    soundMap.put(SoundType.SWORD_SWING, audioNode);
                }
                {
                    AudioNode audioNode = new AudioNode(assetManager, "Sounds/swing/209122__lukesharples__sword-swipe2.wav", AudioData.DataType.Buffer);
                    audioNode.setVolume(volume);
                    audioNode.setPitch(pitch);
                    soundMap.put(SoundType.SWORD_SWING, audioNode);
                }
                {
                    AudioNode audioNode = new AudioNode(assetManager, "Sounds/swing/209123__lukesharples__sword-swipe13.wav", AudioData.DataType.Buffer);
                    audioNode.setVolume(volume);
                    audioNode.setPitch(pitch);
                    soundMap.put(SoundType.SWORD_SWING, audioNode);
                }
                {
                    AudioNode audioNode = new AudioNode(assetManager, "Sounds/swing/209124__lukesharples__sword-swipe4.wav", AudioData.DataType.Buffer);
                    audioNode.setVolume(volume);
                    audioNode.setPitch(pitch);
                    soundMap.put(SoundType.SWORD_SWING, audioNode);
                }
                {
                    AudioNode audioNode = new AudioNode(assetManager, "Sounds/swing/209125__lukesharples__sword-swipe3.wav", AudioData.DataType.Buffer);
                    audioNode.setVolume(volume);
                    audioNode.setPitch(pitch);
                    soundMap.put(SoundType.SWORD_SWING, audioNode);
                }
                {
                    AudioNode audioNode = new AudioNode(assetManager, "Sounds/swing/209126__lukesharples__sword-swipe6.wav", AudioData.DataType.Buffer);
                    audioNode.setVolume(volume);
                    audioNode.setPitch(pitch);
                    soundMap.put(SoundType.SWORD_SWING, audioNode);
                }
                {
                    AudioNode audioNode = new AudioNode(assetManager, "Sounds/swing/209127__lukesharples__sword-swipe5.wav", AudioData.DataType.Buffer);
                    audioNode.setVolume(volume);
                    audioNode.setPitch(pitch);
                    soundMap.put(SoundType.SWORD_SWING, audioNode);
                }
            }
        },
        SWORD_HIT {
            @Override
            void init() {
                {
                    AudioNode audioNode = new AudioNode(assetManager, "Sounds/hit/215008__taira-komori__stabbing.ogg", AudioData.DataType.Buffer);
                    audioNode.setVolume(volume);
                    audioNode.setPitch(pitch);
                    soundMap.put(SoundType.SWORD_HIT, audioNode);
                }
            }
        },
        MENU {
            @Override
            void init() {
                AudioNode audioNode = new AudioNode(assetManager, "Sounds/menu/71469__natharra__blink-4-damped.ogg", AudioData.DataType.Buffer);
                audioNode.setPositional(false);

                soundMap.put(SoundType.MENU, audioNode);
            }
        },
        SWORD_BLOCK {
            @Override
            void init() {
                {
                    AudioNode audioNode = new AudioNode(assetManager, "Sounds/block/213696__taira-komori__sword1.ogg", AudioData.DataType.Buffer);
                    audioNode.setVolume(volume);
                    audioNode.setPitch(pitch);
                    soundMap.put(SoundType.SWORD_BLOCK, audioNode);
                }
                {
                    AudioNode audioNode = new AudioNode(assetManager, "Sounds/block/213695__taira-komori__sword2.ogg", AudioData.DataType.Buffer);
                    audioNode.setVolume(volume);
                    audioNode.setPitch(pitch);
                    soundMap.put(SoundType.SWORD_BLOCK, audioNode);
                }
                {
                    AudioNode audioNode = new AudioNode(assetManager, "Sounds/block/213694__taira-komori__sword3.ogg", AudioData.DataType.Buffer);
                    audioNode.setVolume(volume);
                    audioNode.setPitch(pitch);
                    soundMap.put(SoundType.SWORD_BLOCK, audioNode);
                }
            }
        };

        abstract void init();

        public static AudioNode getClone(SoundType wind) {
            return SoundUtils.getRandomObject(soundMap.get(wind)).clone();
        }
    }

    public SoundManager(AssetManager assetManager) {
        SoundManager.assetManager = assetManager;

        initialize();
    }

    public void initialize() {
        EnumSet.allOf(SoundType.class).forEach(SoundType::init);
    }

    public AudioNode getWindNode() {
        return getClone(SoundType.WIND);
    }

    public AudioNode getSwordSwingNode() {
        return getClone(SoundType.SWORD_SWING);
    }

    public AudioNode getSwordHitNode() {
        return getClone(SoundType.SWORD_HIT);
    }

    public AudioNode getSwordBlockNode() {
        return getClone(SoundType.SWORD_BLOCK);
    }

    public AudioNode getFootStepsNode() {
        return getClone(SoundType.FOOTSTEPS);
    }

    @Override
    public AudioNode getMenuNode() {
        return getClone(SoundType.MENU);
    }

    public void update(float tpf) {
        //TODO: play area/situation specific music here
    }

    public void setVolume(float volume) {
        SoundManager.volume = volume;
    }

    public void reInitialize() {
        soundMap.values().forEach(audioNode -> audioNode.setVolume(volume));
    }
}
