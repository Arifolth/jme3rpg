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

package ru.arifolth.game;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.debug.Arrow;

public class Debug {
    private Debug() {
    }

    public static void showNodeAxes(AssetManager am, Node n, float axisLen)
    {
        Vector3f v = new Vector3f(axisLen, 0, 0);
        Arrow a = new Arrow(v);
        Material mat = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Red);
        Geometry geom = new Geometry(n.getName() + "XAxis", a);
        geom.setMaterial(mat);
        n.attachChild(geom);


        //
        v = new Vector3f(0, axisLen, 0);
        a = new Arrow(v);
        mat = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Green);
        geom = new Geometry(n.getName() + "YAxis", a);
        geom.setMaterial(mat);
        n.attachChild(geom);


        //
        v = new Vector3f(0, 0, axisLen);
        a = new Arrow(v);
        mat = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Blue);
        geom = new Geometry(n.getName() + "ZAxis", a);
        geom.setMaterial(mat);
        n.attachChild(geom);
    }
}
