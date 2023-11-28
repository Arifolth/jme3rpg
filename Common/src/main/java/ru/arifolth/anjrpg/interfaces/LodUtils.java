/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2014 - 2023 Alexander Nilov
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

package ru.arifolth.anjrpg.interfaces;

import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.LodControl;
import jme3tools.optimize.LodGenerator;

import java.util.Collection;
import java.util.List;

public class LodUtils {
    private LodUtils() {}

    public static void setUpModelLod(Spatial model) {
        (((Node) model)).getChildren().forEach(LodUtils::createModelLod);
    }

    public static void setUpFirTreeModelLod(Spatial model) {
        //Structure specific just for "Models/Fir1/fir1_androlo.j3o"!
        ((Node) ((Node) model).getChild(0)).getChildren().forEach(LodUtils::createModelLod);
    }

    public static void setUpCharacterModelLod(Spatial model) {
        Geometry geometry = (Geometry) ((Node) model).getChild(0);
        createModelLod(geometry);
    }

    private static void createModelLod(Spatial geometry) {
        LodGenerator lod = new LodGenerator((Geometry) geometry);
        lod.bakeLods(LodGenerator.TriangleReductionMethod.COLLAPSE_COST, 0.25f, 0.50f, 0.75f, 1.0f);
        LodControl lc = new LodControl();
        geometry.addControl(lc);
    }
}
