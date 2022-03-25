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

package ru.arifolth.game.models.factory;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import ru.arifolth.game.CharacterInterface;
import ru.arifolth.game.SoundManagerInterface;

import java.lang.reflect.InvocationTargetException;

public class CharacterFactory<T extends CharacterInterface> implements CharacterFactoryInterface<T> {
    private final SoundManagerInterface soundManager;
    private BulletAppState bulletAppState;
    private AssetManager assetManager;

    public CharacterFactory(BulletAppState bulletAppState, AssetManager assetManager, SoundManagerInterface soundManager) {
        this.bulletAppState = bulletAppState;
        this.assetManager = assetManager;
        this.soundManager = soundManager;
    }

    public T createCharacter(Class<T> clazz) {
        T result = null;
        try {
            result = clazz.getDeclaredConstructor().newInstance();
            result.initialize(bulletAppState, assetManager, soundManager);
        } catch (IllegalAccessException | NoSuchMethodException | InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return result;
    }
}

