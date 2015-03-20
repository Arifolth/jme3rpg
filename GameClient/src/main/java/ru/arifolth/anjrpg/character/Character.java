package ru.arifolth.anjrpg.character;

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
    public void simpleUpdate(float tpf);
    public void initialize(BulletAppState bulletAppState, AssetManager assetManager);
    public Spatial getCharacterModel();
    public Node getNode();
}
