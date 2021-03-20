/**
 *     Copyright (C) 2021  Alexander Nilov
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

package ru.arifolth.game.models;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.scene.Spatial;

public abstract class GameCharacter implements Character {
    protected BulletAppState bulletAppState;
    protected AssetManager assetManager;

    protected CharacterControl characterControl;
    protected Spatial characterModel;

    public GameCharacter() {
    }

    public void initialize(BulletAppState bulletAppState, AssetManager assetManager) {
        this.bulletAppState = bulletAppState;
        this.assetManager = assetManager;
    }

    public Spatial getCharacterModel() {
        return characterModel;
    }

    public abstract boolean isAttacking();

    public abstract boolean isBlocking();
}
