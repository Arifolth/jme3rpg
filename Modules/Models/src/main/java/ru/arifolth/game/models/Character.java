package ru.arifolth.game.models;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 20.12.12
 * Time: 22:30
 * To change this template use File | Settings | File Templates.
 */
public interface Character {
    void simpleUpdate(float tpf);
    void initialize(BulletAppState bulletAppState, AssetManager assetManager);
    Spatial getCharacterModel();
    Node getNode();
}
