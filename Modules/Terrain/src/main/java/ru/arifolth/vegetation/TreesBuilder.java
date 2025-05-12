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

package ru.arifolth.vegetation;

import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.terrain.geomipmap.TerrainQuad;
import jme3tools.optimize.GeometryBatchFactory;
import ru.arifolth.anjrpg.interfaces.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

import static ru.arifolth.anjrpg.interfaces.Constants.RAY_DOWN;

public class TreesBuilder implements BuilderInterface {

    private final Vector3f quadLocation;
    private final TerrainQuad quad;
    private final ContextInterface context;

    private final Node node;
    private final CountDownLatch countDownLatch;

    public TreesBuilder(Vector3f quadLocation, TerrainQuad quad, ContextInterface context, CountDownLatch countDownLatch) {
        this.quadLocation = quadLocation;
        this.quad = quad;
        this.context = context;
        this.countDownLatch = countDownLatch;

        node = new Node(quad.getName() + ":" + this.toString());
    }

    @Override
    public void run() {
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

        try {
            List<Spatial> quadForest = setupTrees();

            quadForest.forEach(this::accept);

            context.getNode().attachChild(GeometryBatchFactory.optimize(node));
        } finally {
            countDownLatch.countDown();
        }
    }

    private List<Spatial> setupTrees() {
        int forestSize = 350;
        List<Spatial> quadForest = new ArrayList<>(forestSize);
        for (int i = 0; i < forestSize; i++) {
            Node treeModelCustom = TreeTypeEnum.getRandomTree();
            treeModelCustom.scale(1 + Utils.getRandomNumberInRange(1, 10), 1 + Utils.getRandomNumberInRange(1, 10), 1 + Utils.getRandomNumberInRange(1, 10));
            quadForest.add(treeModelCustom);
        }

        return quadForest;
    }

    @Override
    public void accept(Spatial treeNode) {
        CollisionResults results = new CollisionResults();

        try (LockGuard ignored = new LockGuard(context.getQuadLock())) {
            // critical section
            Vector3f start = new Vector3f(quadLocation.x + Utils.getRandomNumberInRange(-Constants.TREE_PLANTING_RANGE, Constants.TREE_PLANTING_RANGE), Constants.TREE_PLANTING_HEIGHT, quadLocation.z + Utils.getRandomNumberInRange(-Constants.TREE_PLANTING_RANGE, Constants.TREE_PLANTING_RANGE));
            Ray ray = new Ray(start, RAY_DOWN);

            quad.collideWith(ray, results);
        }
        CollisionResult hit = results.getClosestCollision();
        if (hit != null) {
            if (hit.getContactPoint().y > Constants.WATER_LEVEL_HEIGHT) {
                Vector3f plantLocation = new Vector3f(hit.getContactPoint().x, hit.getContactPoint().y, hit.getContactPoint().z);
                treeNode.setLocalTranslation(plantLocation.x, plantLocation.y - 3f, plantLocation.z);
                treeNode.setLocalRotation(new Quaternion().fromAngleAxis(Utils.getRandomNumberInRange(-6.5f, 6.5f) * FastMath.DEG_TO_RAD, new Vector3f(1, 0, 1)));

                treeNode.setLocalRotation(new Quaternion().fromAngleAxis(Utils.getRandomNumberInRange(0f, 360f) * FastMath.DEG_TO_RAD, new Vector3f(0, 1, 0)));

                node.attachChild(treeNode);
            }
        }
    }
}
