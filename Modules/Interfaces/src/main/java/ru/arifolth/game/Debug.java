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

    public static final String UNSHADED = "Common/MatDefs/Misc/Unshaded.j3md";

    private Debug() {
    }

    public static void showNodeAxes(AssetManager assetManager, Node node, float axisLen)
    {
        //X
        Vector3f vector3f = new Vector3f(axisLen, 0, 0);
        Arrow arrow = new Arrow(vector3f);
        Material material = new Material(assetManager, UNSHADED);
        material.setColor("Color", ColorRGBA.Red);
        Geometry geometry = new Geometry(node.getName() + "XAxis", arrow);
        geometry.setMaterial(material);
        node.attachChild(geometry);


        //Y
        vector3f = new Vector3f(0, axisLen, 0);
        arrow = new Arrow(vector3f);
        material = new Material(assetManager, UNSHADED);
        material.setColor("Color", ColorRGBA.Green);
        geometry = new Geometry(node.getName() + "YAxis", arrow);
        geometry.setMaterial(material);
        node.attachChild(geometry);


        //Z
        vector3f = new Vector3f(0, 0, axisLen);
        arrow = new Arrow(vector3f);
        material = new Material(assetManager, UNSHADED);
        material.setColor("Color", ColorRGBA.Blue);
        geometry = new Geometry(node.getName() + "ZAxis", arrow);
        geometry.setMaterial(material);
        node.attachChild(geometry);
    }
}
