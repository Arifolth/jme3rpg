/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2021 Alexander Nilov
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

import com.jme3.bullet.control.CharacterControl;
import ru.arifolth.game.CharacterInterface;

import java.util.Random;

public class Utils {
    private Utils() {
    }

    static public int getRandomNumber(int min, int max) {
        Random random = new Random();
        return random.ints(min, max).findFirst().getAsInt();
    }

    static void enableEntityPhysics(CharacterInterface playerCharacter) {
        CharacterControl playerCharacterControl = playerCharacter.getCharacterControl();
        playerCharacterControl.setJumpSpeed(20);
        playerCharacterControl.setFallSpeed(300);
        playerCharacterControl.setGravity(30);
    }
}
