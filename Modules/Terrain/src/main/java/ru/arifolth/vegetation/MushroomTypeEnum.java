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

import com.jme3.asset.AssetManager;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.util.mikktspace.MikktspaceTangentGenerator;
import ru.arifolth.anjrpg.interfaces.BaseVegetationType;
import ru.arifolth.anjrpg.interfaces.LodUtils;
import ru.arifolth.anjrpg.interfaces.Utils;

public enum MushroomTypeEnum implements BaseVegetationType {
    FLY_AGARIC {
        private float scale = 0.0055f;

        private Node mushroom = null;
        final int probability = 40;
        @Override
        public void init() {
            Node mushroomNode = (Node) assetManager.loadModel("Models/Mushrooms/fly_agaric_mushroom/scene.gltf");
            mushroomNode.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);

            LodUtils.setUpModelLod(mushroomNode);
            MikktspaceTangentGenerator.generate(mushroomNode);

            mushroom = mushroomNode;
        }

        @Override
        public Node getNode() {
            Node node = (Node) mushroom.clone();
            Vector3f scale3f = new Vector3f(scale, scale, scale);
            node.setLocalScale(scale3f.x, scale3f.y, scale3f.z);
            node.scale(1 + Utils.getRandomNumberInRange(1, 5), 1 + Utils.getRandomNumberInRange(1, 5), 1 + Utils.getRandomNumberInRange(1, 5));
            return node;
        }

        @Override
        public int getProbability() {
            return probability;
        }
    },
    CLITOCYBE_NEBULARIS {
        private float scale = 4f;

        private Node mushroom = null;
        final int probability = 40;
        @Override
        public void init() {
            Node mushroomNode = (Node) assetManager.loadModel("Models/Mushrooms/clitocybe_nebularis/scene.gltf");
            mushroomNode.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);

            LodUtils.setUpModelLod(mushroomNode);
            MikktspaceTangentGenerator.generate(mushroomNode);

            mushroom = mushroomNode;
        }

        @Override
        public Node getNode() {
            Node node = (Node) mushroom.clone();
            Vector3f scale3f = new Vector3f(scale, scale, scale);
            node.setLocalScale(scale3f.x + Utils.getRandomNumberInRange(5, 15), scale3f.y + Utils.getRandomNumberInRange(5, 15), scale3f.z + Utils.getRandomNumberInRange(5, 15));
            return node;
        }

        @Override
        public int getProbability() {
            return probability;
        }
    },
    MACROLEPIOTA_PROCERA {
        private float scale = 2.55f;

        private Node mushroom = null;
        final int probability = 50;
        @Override
        public void init() {
            Node mushroomNode = (Node) assetManager.loadModel("Models/Mushrooms/macrolepiota_procera/scene.gltf");
            mushroomNode.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);

            LodUtils.setUpModelLod(mushroomNode);
            MikktspaceTangentGenerator.generate(mushroomNode);

            mushroom = mushroomNode;
        }

        @Override
        public Node getNode() {
            Node node = (Node) mushroom.clone();
            Vector3f scale3f = new Vector3f(scale, scale, scale);
            node.setLocalScale(scale3f.x + Utils.getRandomNumberInRange(1, 10), scale3f.y + Utils.getRandomNumberInRange(1, 10), scale3f.z + Utils.getRandomNumberInRange(1, 10));
            return node;
        }

        @Override
        public int getProbability() {
            return probability;
        }
    },
    MUSHROOM {
        private float scale = 0.325f;

        private Node mushroom = null;
        final int probability = 80;
        @Override
        public void init() {
            Node mushroomNode = (Node) assetManager.loadModel("Models/Mushrooms/mushroom/scene.gltf");
            mushroomNode.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);

            LodUtils.setUpModelLod(mushroomNode);
            MikktspaceTangentGenerator.generate(mushroomNode);

            mushroom = mushroomNode;
        }

        @Override
        public Node getNode() {
            Node node = (Node) mushroom.clone();
            Vector3f scale3f = new Vector3f(scale, scale, scale);
            node.setLocalScale(scale3f.x, scale3f.y, scale3f.z);
            node.scale(1 + Utils.getRandomNumberInRange(1, 10), 1 + Utils.getRandomNumberInRange(1, 10), 1 + Utils.getRandomNumberInRange(1, 10));
            return node;
        }

        @Override
        public int getProbability() {
            return probability;
        }
    };

    private static AssetManager assetManager;

    public static void setAssetManager(AssetManager assetManager) {
        MushroomTypeEnum.assetManager = assetManager;
    }

    public static Node getRandomMushroom() {
        Node node = null;

        if(Utils.getRandom(FLY_AGARIC.getProbability())) {
            node = FLY_AGARIC.getNode();
        } else if (Utils.getRandom(MACROLEPIOTA_PROCERA.getProbability())) {
            node = MACROLEPIOTA_PROCERA.getNode();
        } else if (Utils.getRandom(CLITOCYBE_NEBULARIS.getProbability())) {
            node = CLITOCYBE_NEBULARIS.getNode();
        } else {
            node = MUSHROOM.getNode();
        }

        return node;
    }
}
