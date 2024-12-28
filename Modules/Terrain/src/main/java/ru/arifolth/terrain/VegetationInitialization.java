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

package ru.arifolth.terrain;

import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.terrain.geomipmap.TerrainQuad;
import ru.arifolth.anjrpg.interfaces.*;
import ru.arifolth.vegetation.BushBuilder;
import ru.arifolth.vegetation.GrassBuilder;
import ru.arifolth.vegetation.MushroomBuilder;
import ru.arifolth.vegetation.TreesBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VegetationInitialization implements VegetationInitializationInterface {
    private final ExecutorService executorService = Executors.newWorkStealingPool();


    private GameLogicCoreInterface gameLogicCore;

    public VegetationInitialization() {
        Runtime.getRuntime().addShutdownHook(new Thread(executorService::shutdownNow));
    }

    public void setGameLogicCore(GameLogicCoreInterface gameLogicCore) {
        this.gameLogicCore = gameLogicCore;
    }

    @Override
    public void positionTrees(TerrainQuad quad) {
        internalPositionTrees(quad);
    }

    @Override
    public void positionGrass(TerrainQuad quad) {
        internalPositionGrass(quad);
    }

    private void internalPositionTrees(TerrainQuad quad) {
        ContextInterface context = new TreesContext(quad.getUserData(Constants.QUAD_FOREST));

        final Vector3f quadLocation = gameLogicCore.getPlayerCharacter().getCharacterControl().getPhysicsLocation();
        if (context.getNode() == null) {
            context.setNode(new Node());

            context.getNode().setShadowMode(RenderQueue.ShadowMode.Cast);
            context.getNode().setCullHint(Spatial.CullHint.Dynamic);

            executorService.execute(new TreesBuilder(gameLogicCore, quadLocation, quad, context));
            executorService.execute(new TreesBuilder(gameLogicCore, quadLocation, quad, context));
            executorService.execute(new TreesBuilder(gameLogicCore, quadLocation, quad, context));
            executorService.execute(new TreesBuilder(gameLogicCore, quadLocation, quad, context));
        } else {
            gameLogicCore.getApp().enqueue(() -> {
                gameLogicCore.getForestNode().attachChild(context.getNode());
            });
        }
    }

    private void internalPositionMushrooms(TerrainQuad quad) {
        ContextInterface context = new MushroomContext(quad.getUserData(Constants.QUAD_MUSHROOMS));

        final Vector3f quadLocation = gameLogicCore.getPlayerCharacter().getCharacterControl().getPhysicsLocation();
        if (context.getNode() == null) {
            context.setNode(new Node());

            context.getNode().setShadowMode(RenderQueue.ShadowMode.Receive);
            context.getNode().setQueueBucket(RenderQueue.Bucket.Transparent);
            context.getNode().setCullHint(Spatial.CullHint.Dynamic);

            executorService.execute(new MushroomBuilder(gameLogicCore, quadLocation, quad, context));
        } else {
            gameLogicCore.getApp().enqueue(() -> {
                gameLogicCore.getMushroomsNode().attachChild(context.getNode());
            });
        }
    }

    private void internalPositionGrass(TerrainQuad quad) {
        ContextInterface context = new GrassContext(quad.getUserData(Constants.QUAD_GRASS));

        final Vector3f quadLocation = gameLogicCore.getPlayerCharacter().getCharacterControl().getPhysicsLocation();
        if (context.getNode() == null) {
            context.setNode(new Node());

            context.getNode().setShadowMode(RenderQueue.ShadowMode.Receive);
            context.getNode().setQueueBucket(RenderQueue.Bucket.Transparent);
            context.getNode().setCullHint(Spatial.CullHint.Dynamic);

            executorService.execute(new GrassBuilder(gameLogicCore, quadLocation, quad, context));
        } else {
            gameLogicCore.getApp().enqueue(() -> {
                gameLogicCore.getGrassNode().attachChild(context.getNode());
            });
        }
    }

    @Override
    public void positionBushes(TerrainQuad quad) {
        internalPositionBushes(quad);
    }

    @Override
    public void positionMushrooms(TerrainQuad quad) {
        internalPositionMushrooms(quad);
    }

    private void internalPositionBushes(TerrainQuad quad) {
        ContextInterface context = new BushesContext(quad.getUserData(Constants.QUAD_BUSHES));

        final Vector3f quadLocation = gameLogicCore.getPlayerCharacter().getCharacterControl().getPhysicsLocation();
        if (context.getNode() == null) {
            context.setNode(new Node());

            context.getNode().setShadowMode(RenderQueue.ShadowMode.Receive);
            context.getNode().setQueueBucket(RenderQueue.Bucket.Transparent);
            context.getNode().setCullHint(Spatial.CullHint.Dynamic);

            executorService.execute(new BushBuilder(gameLogicCore, quadLocation, quad, context));
        } else {
            gameLogicCore.getApp().enqueue(() -> {
                gameLogicCore.getBushesNode().attachChild(context.getNode());
            });
        }
    }
}
