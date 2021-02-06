package ru.arifolth.game.models;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.scene.Spatial;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 21.12.12
 * Time: 0:37
 * To change this template use File | Settings | File Templates.
 */
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


}
