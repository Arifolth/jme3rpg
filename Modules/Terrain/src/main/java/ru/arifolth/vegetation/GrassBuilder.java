/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2014 - 2024 Alexander Nilov
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
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.terrain.geomipmap.TerrainQuad;
import jme3tools.optimize.GeometryBatchFactory;
import ru.arifolth.anjrpg.interfaces.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static ru.arifolth.anjrpg.interfaces.Constants.RAY_DOWN;

public class GrassBuilder implements BuilderInterface {
    private final GameLogicCoreInterface gameLogicCore;

    private final Vector3f quadLocation;
    private final TerrainQuad quad;
    private final ContextInterface context;
    private Node node = new Node();

    public GrassBuilder(GameLogicCoreInterface gameLogicCore, Vector3f quadLocation, TerrainQuad quad, ContextInterface context) {
        this.gameLogicCore = gameLogicCore;
        this.quadLocation = quadLocation;
        this.quad = quad;
        this.context = context;
    }

    @Override
    public void run() {
        try {
            List<Spatial> quadGrass = setupGrass();

            Stream<Spatial> stream = quadGrass.stream();
            stream.forEach(this::accept);

            setNode(GeometryBatchFactory.optimize(getNode(), true));
            synchronized (quadLocation) {
                context.getNode().attachChild(getNode());
                context.getNode().updateModelBound();

                quad.setUserData(Constants.QUAD_GRASS, context.getNode());
            }
        } finally {
            gameLogicCore.getApp().enqueue(() -> {
                gameLogicCore.getGrassNode().attachChild(context.getNode());
            });
        }
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public Node getNode() {
        return node;
    }
    private List<Spatial> setupGrass() {
        final int grassAmount = 125_000;
        List<Spatial> quadGrass = new ArrayList<>(grassAmount);
        IntStream.range(0, grassAmount).parallel().mapToObj(i -> GrassTypeEnum.getRandomGrass()).forEach(grassInstance -> {
            grassInstance.setLocalScale(1 + Utils.getRandomNumberInRange(1, 3), 1 + Utils.getRandomNumberInRange(1, 3), 1 + Utils.getRandomNumberInRange(1, 3));
            grassInstance.setLocalTranslation(grassInstance.getLocalTranslation().getX(), grassInstance.getLocalTranslation().getY(), grassInstance.getLocalTranslation().getZ() - 15);
            grassInstance.rotate(Utils.getRandomNumberInRange(-0.65f, 0.65f), Utils.getRandomNumberInRange(-1.65f, 1.65f), 0);
            quadGrass.add(grassInstance);
        });

        return quadGrass;
    }

    @Override
    public void accept(Spatial grassSpatial) {
        CollisionResults results = new CollisionResults();

        synchronized (quadLocation) {
            Vector3f start = new Vector3f(quadLocation.x + Utils.getRandomNumberInRange(-Constants.TREE_PLANTING_RANGE, Constants.TREE_PLANTING_RANGE), Constants.TREE_PLANTING_HEIGHT, quadLocation.z + Utils.getRandomNumberInRange(-Constants.TREE_PLANTING_RANGE, Constants.TREE_PLANTING_RANGE));
            Ray ray = new Ray(start, RAY_DOWN);

            quad.collideWith(ray, results);
        }
        CollisionResult hit = results.getClosestCollision();
        if (hit != null) {
            if ((hit.getContactPoint().y > Constants.WATER_LEVEL_HEIGHT)) {
                Vector3f plantLocation = new Vector3f(hit.getContactPoint().x, hit.getContactPoint().y, hit.getContactPoint().z);
                grassSpatial.setLocalTranslation(plantLocation.x, plantLocation.y, plantLocation.z);

                getNode().attachChild(grassSpatial);
            }
        }
    }
}