package ru.arifolth.anjrpg.character.factory;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import ru.arifolth.anjrpg.character.Character;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 20.12.12
 * Time: 23:16
 * To change this template use File | Settings | File Templates.
 */
public class CharacterFactory<T extends Character> implements ICharacterFactory<T>{
    private BulletAppState bulletAppState;
    private AssetManager assetManager;

    public CharacterFactory(BulletAppState bulletAppState, AssetManager assetManager) {
        this.bulletAppState = bulletAppState;
        this.assetManager = assetManager;
    }

    public T createCharacter(Class<T> clazz) {
        T result = null;
        try {
            result = clazz.newInstance();
            result.initialize(bulletAppState, assetManager);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return result;
    }
}

