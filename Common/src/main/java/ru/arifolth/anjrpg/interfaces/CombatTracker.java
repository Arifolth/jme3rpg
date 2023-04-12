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

import com.jme3.audio.AudioNode;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CombatTracker implements CombatTrackerInterface {
    final private static Logger LOGGER = Logger.getLogger(CombatTracker.class.getName());
    private Set<CharacterInterface> enemies = new HashSet<>();
    private GameLogicCoreInterface gameLogicCore;
    private boolean inCombat = false;

    public CombatTracker() {
    }

    @Override
    public void update(float tpf) {
        inCombat = !enemies.isEmpty();

        if(inCombat && !GameState.BATTLE.equals(gameLogicCore.getGameStateManager().getCurrentGameState())) {
            gameLogicCore.getGameStateManager().setGameState(GameState.BATTLE);
        }

        if(!inCombat && GameState.BATTLE.equals(gameLogicCore.getGameStateManager().getCurrentGameState())) {
            gameLogicCore.getGameStateManager().setGameState(GameState.CALM);
        }
    }

    @Override
    public void decCounter(CharacterInterface enemy) {
        enemies.remove(enemy);
    }

    @Override
    public void incCounter(CharacterInterface enemy) {
        enemies.add(enemy);
    }

    @Override
    public boolean isInCombat() {
        return inCombat;
    }

    @Override
    public void reset() {
        enemies.clear();
        inCombat = false;
    }

    public GameLogicCoreInterface getGameLogicCore() {
        return gameLogicCore;
    }

    public void setGameLogicCore(GameLogicCoreInterface gameLogicCore) {
        if (this.gameLogicCore == null) {
            this.gameLogicCore = gameLogicCore;
        }
    }
}
