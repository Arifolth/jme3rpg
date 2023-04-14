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

package ru.arifolth.anjrpg;

import ru.arifolth.anjrpg.interfaces.Constants;
import ru.arifolth.anjrpg.interfaces.GameLogicCoreInterface;
import ru.arifolth.anjrpg.interfaces.TrackerInterface;

public class LocationTracker implements TrackerInterface {
    private GameLogicCoreInterface gameLogicCore;

    public LocationTracker(GameLogicCoreInterface gameLogicCore) {
        this.gameLogicCore = gameLogicCore;
    }

    @Override
    public void update(float tpf) {
        //TODO: check game state: fight, day/night,
        checkNPCs();

    }

    private void checkNPCs() {
        //TODO: decide if we need to inject one more
        if(gameLogicCore.getCharacterMap().size() < Constants.NPC_AMOUNT) {
            gameLogicCore.getInitializationDelegate().initializeNPCs(true);
        }
        //TODO: freeze if far away from player
        //TODO: check and remove if out of certain range (too far away)
    }

}
