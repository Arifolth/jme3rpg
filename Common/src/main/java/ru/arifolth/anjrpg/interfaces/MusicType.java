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

package ru.arifolth.anjrpg.interfaces;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;

public enum MusicType implements AudioType {
    MENU {
        @Override
        public void init() {
            createAudioNode(this,
                    "Sounds/music/menu/sb_jade.ogg"
                );
        }
    },
    CALM {
        @Override
        public void init() {
            {
                createAudioNode(this,
                        "Sounds/music/calm/sb_dragonslullabye.ogg"
                );
            }
        }
    },
    MOUNTAINS {
        @Override
        public void init() {
            createAudioNode(this,
                    "Sounds/music/mountains/sb_inflection.ogg"
            );
        }
    },
    VILLAGE {
        @Override
        public void init() {
            createAudioNode(this,
                    "Sounds/music/village/sb_legendofkvothe.ogg"
            );
        }
    },
    SNOW {
        @Override
        public void init() {
            createAudioNode(this,
                    "Sounds/music/snow/FirstSnow.ogg"
            );
        }
    },
    DAWN {
        @Override
        public void init() {
            createAudioNode(this,
                    "Sounds/music/dawn/HymnToTheDawn.ogg"
            );
        }
    },
    DUSK {
        @Override
        public void init() {
            createAudioNode(this,
                    "Sounds/music/dusk/PathThroughTheMountains.ogg"
            );
        }
    },
    EXPLORATION {
        @Override
        public void init() {
            createAudioNode(this,
                    "Sounds/music/exploration/Inbound.ogg"
            );
        }
    },
    BATTLE {
        @Override
        public void init() {
            {
                createAudioNode(this,
                        "Sounds/music/battle/Legionnaire2022.ogg"
                );
            }
        }
    },
    DAY {
        @Override
        public void init() {
            {
                createAudioNode(this,
                        "Sounds/music/day/sb_iha_confessions.ogg"
                );
            }
            {
                createAudioNode(this,
                        "Sounds/music/day/sb_iha_desperation.ogg"
                );
            }
            {
                createAudioNode(this,
                        "Sounds/music/day/sb_iha_junkyard.ogg"
                );
            }
            {
                createAudioNode(this,
                        "Sounds/music/day/sb_iha_specialops.ogg"
                );
            }
            {
                createAudioNode(this,
                        "Sounds/music/day/sb_iha_quirkylane.ogg"
                );
            }
            {
                createAudioNode(this,
                        "Sounds/music/day/sb_worldofmagic.ogg"
                );
            }
        }
    },
    DEATH {
        @Override
        public void init() {
            {
                createAudioNode(this,
                        "Sounds/music/death/sb_annastheme.ogg"
                );
            }
            {
                createAudioNode(this,
                        "Sounds/music/death/sb_thesummoning.ogg"
                );
            }
        }
    },
    NIGHT {
        @Override
        public void init() {
            {
                createAudioNode(this,
                        "Sounds/music/night/LightInDarkPlaces2019.ogg"
                );
            }
            {
                createAudioNode(this,
                        "Sounds/music/night/sb_beautifuloblivion.ogg"
                );
            }
            {
                createAudioNode(this,
                        "Sounds/music/night/sb_celestial.ogg"
                );
            }
            {
                createAudioNode(this,
                        "Sounds/music/night/sb_helios.ogg"
                );
            }
        }
    };

    private static AudioData.DataType buffer = AudioData.DataType.Stream;
    private static Float pitch = null;
    private static boolean positional = false;
    private static Boolean looping = false;

    private static AssetManager assetManager;

    public static void setAssetManager(AssetManager assetManager) {
        MusicType.assetManager = assetManager;
    }

    public static AudioNode getClone(AudioType musicType) {
        return SoundUtils.getRandomObject(musicMap.get(musicType)).clone();
    }

    public static AudioNode getOriginal(AudioType musicType) {
        return SoundUtils.getSingleObject(musicMap.get(musicType));
    }

    public static void createAudioNode(AudioType musicType, String name) {
        AudioNode audioNode = new AudioNode(assetManager, name, AudioData.DataType.Stream);
        audioNode.setVolume(Constants.SOUND_VOLUME / 6);
        audioNode.setPositional(false);
        musicMap.put(musicType, audioNode);
    }

    public static void reInitialize(float volume) {
        musicMap.values().forEach(audioNode -> audioNode.setVolume(volume));
    }
    private static final Multimap<AudioType, AudioNode> musicMap = ArrayListMultimap.create();
}
