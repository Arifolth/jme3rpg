/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2014 - 2026 Alexander Nilov
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
    private GameState currentGameState = GameState.MENU;
    private GameState nextGameState;

    public GameStateManager(GameLogicCoreInterface gameLogicCore) {
        this.gameLogicCore = gameLogicCore;
    }

    @Override
    public void update(float tpf) {
        if(nextGameState != null) {
            changeState(Constants.CHANGE_GAME_STATE_TPF);
        } else {
            checkTime();
        }
    }

    @Override
    public void setGameState(GameState gameState) {
        if(currentGameState.isNextAcceptable(gameState)) {
            this.nextGameState = gameState;
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

        if(AudioSource.Status.Stopped.equals(gameLogicCore.getSoundManager().getCurrentMusicNode().getStatus()) ||
                nextGameState.equals(GameState.BATTLE) ||
                nextGameState.equals(GameState.CALM) ||
                nextGameState.equals(GameState.DEATH)) {
            gameLogicCore.getSoundManager().fadeMusicOut(tpf, nextGameState.getMusicType());
        }
        LOGGER.log(Level.INFO, "Change GameState: " + currentGameState + " to " + nextGameState);
        currentGameState = nextGameState;
        gameLogicCore.getSoundManager().changeAmbientSound(tpf, nextGameState.getAmbientSound());
        nextGameState = null;
    }
    @Override
    public void checkTime() {
        int hours = gameLogicCore.getSky().getHours();

        if (Utils.isBetween(hours, 0, 6)) {
            this.setGameState(GameState.NIGHT);
        } else if (Utils.isBetween(hours, 7, 8)) {
            this.setGameState(GameState.DAWN);
        } else if (Utils.isBetween(hours, 9, 18)) {
            this.setGameState(GameState.DAY);
        } else if (Utils.isBetween(hours, 19, 21)) {
            this.setGameState(GameState.DUSK);
        } else if (Utils.isBetween(hours, 22, 23)) {
            this.setGameState(GameState.NIGHT);
        } else {
            throw new RuntimeException("checkTime");
        }
    }
}
