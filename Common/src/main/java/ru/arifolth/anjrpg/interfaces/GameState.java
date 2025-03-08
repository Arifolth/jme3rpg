/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2014 - 2025 Alexander Nilov
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
        @Override
        public SoundTypeEnum getAmbientSound() {
            return null;
        }
    },
    BATTLE {
        @Override
        public MusicTypeEnum getMusicType() {
            return MusicTypeEnum.BATTLE;
        }
        @Override
        public SoundTypeEnum getAmbientSound() {
            return null;
        }
    },
    EXPLORATION {
        @Override
        public MusicTypeEnum getMusicType() {
            return MusicTypeEnum.EXPLORATION;
        }
        @Override
        public SoundTypeEnum getAmbientSound() {
            return null;
        }
    },
    DAY {
        @Override
        public MusicTypeEnum getMusicType() {
            return MusicTypeEnum.DAY;
        }
        @Override
        public SoundTypeEnum getAmbientSound() {
            return SoundTypeEnum.WIND;
        }
    },
    CALM {
        @Override
        public MusicTypeEnum getMusicType() {
            return MusicTypeEnum.CALM;
        }
        @Override
        public SoundTypeEnum getAmbientSound() {
            return null;
        }
    },
    MOUNTAINS {
        @Override
        public MusicTypeEnum getMusicType() {
            return MusicTypeEnum.MOUNTAINS;
        }
        @Override
        public SoundTypeEnum getAmbientSound() {
            return null;
        }
    },
    VILLAGE {
        @Override
        public MusicTypeEnum getMusicType() {
            return MusicTypeEnum.VILLAGE;
        }
        @Override
        public SoundTypeEnum getAmbientSound() {
            return null;
        }
    },
    DEATH {
        @Override
        public MusicTypeEnum getMusicType() {
            return MusicTypeEnum.DEATH;
        }
        @Override
        public SoundTypeEnum getAmbientSound() {
            return null;
        }
    },
    NIGHT {
        @Override
        public MusicTypeEnum getMusicType() {
            return MusicTypeEnum.NIGHT;
        }
        @Override
        public SoundTypeEnum getAmbientSound() {
            return SoundTypeEnum.NIGHT;
        }
    },
    FEAR {
        @Override
        public MusicTypeEnum getMusicType() {
            return null;
        }
        @Override
        public SoundTypeEnum getAmbientSound() {
            return null;
        }
    },
    DAWN {
        @Override
        public MusicTypeEnum getMusicType() {
            return MusicTypeEnum.DAWN;
        }
        @Override
        public SoundTypeEnum getAmbientSound() {
            return SoundTypeEnum.WIND;
        }
    },
    DUSK {
        @Override
        public MusicTypeEnum getMusicType() {
            return MusicTypeEnum.DUSK;
        }
        @Override
        public SoundTypeEnum getAmbientSound() {
            return SoundTypeEnum.WIND;
        }
    };
    public abstract MusicTypeEnum getMusicType();

    public boolean isNextAcceptable(GameState gameState) {
        boolean result = false;

        if(!this.equals(gameState)) {
            switch (this) {
                case MENU -> {
                    result = switch (gameState) {
                        case CALM, NIGHT, DAY, DAWN, DUSK -> true;
                        default -> false;
                    };
                }
                case NIGHT -> {
                    result = switch (gameState) {
                        case DAWN, BATTLE -> true;
                        default -> false;
                    };
                }
                case DAY -> {
                    result = switch (gameState) {
                        case DUSK, BATTLE -> true;
                        default -> false;
                    };
                }
                case DAWN -> {
                    result = switch (gameState) {
                        case DAY, BATTLE -> true;
                        default -> false;
                    };
                }
                case DUSK -> {
                    result = switch (gameState) {
                        case NIGHT, BATTLE -> true;
                        default -> false;
                    };
                }
                case CALM -> {
                    result = switch (gameState) {
                        case NIGHT, DAY, DAWN, DUSK, BATTLE -> true;
                        default -> false;
                    };
                }
                case BATTLE -> {
                    result = gameState.equals(CALM) || gameState.equals(DEATH);
                }
                case DEATH -> {
                    result = gameState.equals(CALM);
                }
            }
        }

        return result;
    }

    public abstract SoundTypeEnum getAmbientSound();
}
