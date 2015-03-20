package ru.arifolth.anjrpg.weather;

import com.jme3.scene.Spatial;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 27.12.12
 * Time: 1:39
 * To change this template use File | Settings | File Templates.
 */
public interface Emitter {
    void setSpatial(Spatial spatial);
    void simpleUpdate(float tpf);
}
