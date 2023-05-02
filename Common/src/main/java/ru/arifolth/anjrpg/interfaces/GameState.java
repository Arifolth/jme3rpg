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

package ru.arifolth.anjrpg.interfaces;

public enum GameState {
    MENU {
        @Override
        public MusicType getMusicType() {
            return MusicType.MENU;
        }
    },
    BATTLE {
        @Override
        public MusicType getMusicType() {
            return MusicType.BATTLE;
        }
    },
    EXPLORATION {
        @Override
        public MusicType getMusicType() {
            return MusicType.EXPLORATION;
        }
    },
    DAY {
        @Override
        public MusicType getMusicType() {
            return MusicType.DAY;
        }
    },
    CALM {
        @Override
        public MusicType getMusicType() {
            return MusicType.CALM;
        }
    },
    MOUNTAINS {
        @Override
        public MusicType getMusicType() {
            return MusicType.MOUNTAINS;
        }
    },
    VILLAGE {
        @Override
        public MusicType getMusicType() {
            return MusicType.VILLAGE;
        }
    },
    DEATH {
        @Override
        public MusicType getMusicType() {
            return MusicType.DEATH;
        }
    },
    NIGHT {
        @Override
        public MusicType getMusicType() {
            return MusicType.NIGHT;
        }
    },
    FEAR {
        @Override
        public MusicType getMusicType() {
            return null;
        }
    },
    SNOW {
        @Override
        public MusicType getMusicType() {
            return MusicType.SNOW;
        }
    },
    RAIN {
        @Override
        public MusicType getMusicType() {
            return MusicType.SNOW;
        }
    },
    DAWN {
        @Override
        public MusicType getMusicType() {
            return MusicType.DAWN;
        }
    },
    DUSK {
        @Override
        public MusicType getMusicType() {
            return MusicType.DUSK;
        }
    };
    public abstract MusicType getMusicType();
}
