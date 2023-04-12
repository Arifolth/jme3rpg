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

import com.jme3.audio.AudioSource;

import java.util.logging.Level;
import java.util.logging.Logger;

public class GameStateManager implements GameStateManagerInterface {
    final private static Logger LOGGER = Logger.getLogger(GameStateManager.class.getName());

    private GameLogicCoreInterface gameLogicCore;
    private GameState currentGameState = GameState.MENU;;
    private GameState nextGameState;

    public GameStateManager(GameLogicCoreInterface gameLogicCore) {
        this.gameLogicCore = gameLogicCore;
    }

    @Override
    public void update(float tpf) {
        if(nextGameState != null) {
            LOGGER.log(Level.INFO, "next GameState:" + nextGameState);

            switch (nextGameState) {
                case CALM: {
                    checkTime();
                    changeState(0.3f);
                    break;
                }
                case NIGHT:
                case DAWN:
                case DAY:
                case DUSK:
                case DEATH:
                {
                    changeState(0.3f);
                    break;
                }
                case BATTLE: {
                    changeState(0.3f);
                    break;
                }
                default: {
                    if(AudioSource.Status.Stopped.equals(gameLogicCore.getSoundManager().getCurrentMusicNode().getStatus())) {
                        checkTime();
                        changeState(0.3f);
                    }
                }
            }
        } else {
            if(AudioSource.Status.Stopped.equals(gameLogicCore.getSoundManager().getCurrentMusicNode().getStatus())) {
                checkTime();
                changeState(0.3f);
            }
        }
    }

    @Override
    public void setGameState(GameState gameState) {
        if(!gameState.equals(nextGameState) && !gameState.equals(currentGameState)) {
//            implement isNextAcceptable()
            if (gameState.equals(GameState.CALM) || !currentGameState.equals(GameState.DEATH)) {
                this.nextGameState = gameState;
            }
        }
    }

    @Override
    public GameState getCurrentGameState() {
        return currentGameState;
    }

    @Override
    public void changeState(float tpf) {
        if(nextGameState == null)
            return;

        gameLogicCore.getSoundManager().fadeMusicOut(tpf, nextGameState.getMusicType());
        currentGameState = nextGameState;
        LOGGER.log(Level.INFO, "change GameState:" + currentGameState);
        nextGameState = null;
    }
    @Override
    public void checkTime() {
        int hours = gameLogicCore.getSky().getHours();

        GameStateManagerInterface gameStateManager = gameLogicCore.getGameStateManager();
        if (Utils.isBetween(hours, 0, 6)) {
            gameStateManager.setGameState(GameState.NIGHT);
        } else if (Utils.isBetween(hours, 7, 8)) {
            gameStateManager.setGameState(GameState.DAWN);
        } else if (Utils.isBetween(hours, 9, 18)) {
            gameStateManager.setGameState(GameState.DAY);
        } else if (Utils.isBetween(hours, 19, 21)) {
            gameStateManager.setGameState(GameState.DUSK);
        } else if (Utils.isBetween(hours, 22, 23)) {
            gameStateManager.setGameState(GameState.NIGHT);
        } else {
            throw new RuntimeException("checkTime");
        }
    }
}
