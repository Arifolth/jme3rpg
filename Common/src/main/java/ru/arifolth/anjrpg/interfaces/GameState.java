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
        public MusicTypeEnum getMusicType() {
            return MusicTypeEnum.MENU;
        }
    },
    BATTLE {
        @Override
        public MusicTypeEnum getMusicType() {
            return MusicTypeEnum.BATTLE;
        }
    },
    EXPLORATION {
        @Override
        public MusicTypeEnum getMusicType() {
            return MusicTypeEnum.EXPLORATION;
        }
    },
    DAY {
        @Override
        public MusicTypeEnum getMusicType() {
            return MusicTypeEnum.DAY;
        }
    },
    CALM {
        @Override
        public MusicTypeEnum getMusicType() {
            return MusicTypeEnum.CALM;
        }
    },
    MOUNTAINS {
        @Override
        public MusicTypeEnum getMusicType() {
            return MusicTypeEnum.MOUNTAINS;
        }
    },
    VILLAGE {
        @Override
        public MusicTypeEnum getMusicType() {
            return MusicTypeEnum.VILLAGE;
        }
    },
    DEATH {
        @Override
        public MusicTypeEnum getMusicType() {
            return MusicTypeEnum.DEATH;
        }
    },
    NIGHT {
        @Override
        public MusicTypeEnum getMusicType() {
            return MusicTypeEnum.NIGHT;
        }
    },
    FEAR {
        @Override
        public MusicTypeEnum getMusicType() {
            return null;
        }
    },
    SNOW {
        @Override
        public MusicTypeEnum getMusicType() {
            return MusicTypeEnum.SNOW;
        }
    },
    RAIN {
        @Override
        public MusicTypeEnum getMusicType() {
            return MusicTypeEnum.SNOW;
        }
    },
    DAWN {
        @Override
        public MusicTypeEnum getMusicType() {
            return MusicTypeEnum.DAWN;
        }
    },
    DUSK {
        @Override
        public MusicTypeEnum getMusicType() {
            return MusicTypeEnum.DUSK;
        }
    };
    public abstract MusicTypeEnum getMusicType();
}
