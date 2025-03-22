/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2014 - 2025 Alexander Nilov
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

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.bounding.BoundingVolume;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.LodControl;
import jme3tools.optimize.LodGenerator;

public class LodUtils {
    private LodUtils() {}

    // Static visitor instance to avoid repeated object allocation
    private static final SceneGraphVisitor LOD_VISITOR = new SceneGraphVisitor() {
        @Override
        public void visit(Spatial spatial) {
            if(spatial instanceof Node node) {
                if(node.getChildren() != null && !node.getChildren().isEmpty()) {
                    node.getChildren().forEach(this::visit);
                }
            } else if (spatial instanceof Geometry geometry) {
                createModelLod(geometry);
            }
        }
    };

    public static void setUpModelLod(Spatial model) {
        // Use breadth-first traversal for potential better cache locality
        model.breadthFirstTraversal(LOD_VISITOR);
    }

    private static void createModelLod(Geometry geometry) {
        // Generate LODs with optimized parameters
        LodGenerator lod = new LodGenerator(geometry);

        // Fewer LOD levels with aggressive reduction
        lod.bakeLods(
                // Aggressive LODs: Keep 70% → 30% → 10% → 3% → 1% triangles
                LodGenerator.TriangleReductionMethod.COLLAPSE_COST, 0.7f, 0.3f, 0.1f, 0.03f, 0.01f);

        LodControl control = new LodControl();
        control.setDistTolerance(calculateObjectSize(geometry) * 10f); // Larger objects switch sooner
        geometry.addControl(control);
    }

    public static float calculateObjectSize(Spatial spatial) {
        BoundingVolume bv = spatial.getWorldBound();

        if (bv instanceof BoundingBox) {
            // For box-shaped objects
            BoundingBox box = (BoundingBox) bv;
            Vector3f extents = box.getExtent(new Vector3f());
            return extents.length() * 2; // Diagonal length
        } else if (bv instanceof BoundingSphere) {
            // For sphere-shaped objects
            BoundingSphere sphere = (BoundingSphere) bv;
            return sphere.getRadius() * 2; // Diameter
        }

        return 0f;
    }
}
