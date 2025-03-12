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
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.util.mikktspace.MikktspaceTangentGenerator;
import ru.arifolth.anjrpg.interfaces.BaseVegetationType;
import ru.arifolth.anjrpg.interfaces.LodUtils;
import ru.arifolth.anjrpg.interfaces.Utils;

public enum TreeTypeEnum implements BaseVegetationType {
    BOULDER {
        Node tree = null;
        final int probability = 5;
        @Override
        public void init() {
            Node rockNode = (Node) assetManager.loadModel("Models/Rock1/rock1_nobiax.j3o");
            rockNode.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);

            MikktspaceTangentGenerator.generate(rockNode);
            LodUtils.setUpModelLod(rockNode);

            tree = rockNode;
        }

        @Override
        public Node getNode() {
            Node node = (Node) tree.clone();
            node.setLocalScale(Utils.getRandomNumberInRange(-3, 3), Utils.getRandomNumberInRange(-3, 3), Utils.getRandomNumberInRange(-3, 3));
            return node;
        }

        @Override
        public int getProbability() {
            return probability;
        }
    },
    FIR {
        Node tree = null;
        final int probability = 60;
        @Override
        public void init() {
            Node firTree = (Node) assetManager.loadModel("Models/Fir1/fir1_androlo.j3o");
            firTree.setShadowMode(RenderQueue.ShadowMode.Cast);

            MikktspaceTangentGenerator.generate(firTree);
            LodUtils.setUpModelLod(firTree);

            tree = firTree;
        }

        @Override
        public Node getNode() {
            Node node = (Node) tree.clone();
            node.setLocalScale(Utils.getRandomNumberInRange(1, 4), Utils.getRandomNumberInRange(1, 2), Utils.getRandomNumberInRange(1, 2));
            return node;
        }

        @Override
        public int getProbability() {
            return probability;
        }
    },
    TREE {
        Node tree = null;
        final int probability = 75;
        @Override
        public void init() {
            Node tree = (Node) assetManager.loadModel("Models/Trees/tree_low-poly/scene.gltf");
            tree.setShadowMode(RenderQueue.ShadowMode.Cast);

            LodUtils.setUpModelLod(tree);
            MikktspaceTangentGenerator.generate(tree);

            this.tree = tree;
        }

        @Override
        public Node getNode() {
            Node node = (Node) tree.clone();
            float scale = 0.25f;
            Vector3f scale3f = new Vector3f(scale, scale, scale);
//            node.setLocalScale(scale3f.x, scale3f.y, scale3f.z);
            node.setLocalScale(scale3f.x + Utils.getRandomNumberInRange(-0.125f, 0.125f), scale3f.y + Utils.getRandomNumberInRange(-0.125f, 0.125f), scale3f.z + Utils.getRandomNumberInRange(-0.125f, 0.125f));
            return node;
        }

        @Override
        public int getProbability() {
            return probability;
        }
    },
    TREE2 {
        Node tree = null;
        final int probability = 65;
        @Override
        public void init() {
            Node tree2 = (Node) assetManager.loadModel("Models/Trees/tree2/scene.gltf");
            tree2.setShadowMode(RenderQueue.ShadowMode.Cast);

            MikktspaceTangentGenerator.generate(tree2);
            LodUtils.setUpModelLod(tree2);

            tree = tree2;
        }

        @Override
        public Node getNode() {
            Node node = (Node) tree.clone();
            float scale = 0.0125f;
            Vector3f scale3f = new Vector3f(scale, scale, scale);
            node.setLocalScale(scale3f.x + Utils.getRandomNumberInRange(-0.008f, 0.008f), scale3f.y + Utils.getRandomNumberInRange(-0.008f, 0.008f), scale3f.z + Utils.getRandomNumberInRange(-0.008f, 0.008f));
            return node;
        }

        @Override
        public int getProbability() {
            return probability;
        }
    },
    TREE3 {
        Node tree = null;
        final int probability = 5;
        @Override
        public void init() {
            Node tree3 = (Node) assetManager.loadModel("Models/Trees/tree3/scene.gltf");
            tree3.setShadowMode(RenderQueue.ShadowMode.Cast);

            MikktspaceTangentGenerator.generate(tree3);
            LodUtils.setUpModelLod(tree3);

            tree = tree3;
        }

        @Override
        public Node getNode() {
            Node node = (Node) tree.clone();
            float scale = 0.0125f;
            Vector3f scale3f = new Vector3f(scale, scale, scale);
            node.setLocalScale(scale3f.x + Utils.getRandomNumberInRange(-0.008f, 0.008f), scale3f.y + Utils.getRandomNumberInRange(-0.008f, 0.008f), scale3f.z + Utils.getRandomNumberInRange(-0.008f, 0.008f));
            return node;
        }

        @Override
        public int getProbability() {
            return probability;
        }
    },
    MAPPLE {
        Node tree = null;
        final int probability = 40;
        @Override
        public void init() {
            Node mapleTree = (Node) assetManager.loadModel("Models/Maple/tree_maple.j3o");
            Geometry mapleTrunk = (Geometry) mapleTree.getChild("maple trunk");
            mapleTrunk.setMaterial(assetManager.loadMaterial("Models/Maple/Maple_Trunk.j3m"));

            Geometry mapleLeaves = (Geometry) mapleTree.getChild("maple leaves");
            mapleLeaves.setMaterial(assetManager.loadMaterial("Models/Maple/Maple_Leaves.j3m"));

            mapleTree.setShadowMode(RenderQueue.ShadowMode.Cast);

            MikktspaceTangentGenerator.generate(mapleTree);
            LodUtils.setUpModelLod(mapleTree);

            tree = mapleTree;
        }

        @Override
        public Node getNode() {
            Node node = (Node) tree.clone();
            node.setLocalScale(Utils.getRandomNumberInRange(1, 2), Utils.getRandomNumberInRange(1, 2), Utils.getRandomNumberInRange(1, 3));
            return node;
        }

        @Override
        public int getProbability() {
            return probability;
        }
    },
    OAK {
        Node tree = null;
        final int probability = 70;
        @Override
        public void init() {
            Node oakTree = (Node) assetManager.loadModel("Models/Oak/tree_oak.j3o");
            Geometry oakTrunk = (Geometry) oakTree.getChild("oak trunk");
            oakTrunk.setMaterial(assetManager.loadMaterial("Models/Oak/Oak_Trunk.j3m"));

            Geometry oakLeaves = (Geometry) oakTree.getChild("oak leaves");
            oakLeaves.setMaterial(assetManager.loadMaterial("Models/Oak/Oak_Leaves.j3m"));
            oakTree.setShadowMode(RenderQueue.ShadowMode.Cast);

            MikktspaceTangentGenerator.generate(oakTree);
            LodUtils.setUpModelLod(oakTree);

            tree = oakTree;
        }

        @Override
        public Node getNode() {
            Node node = (Node) tree.clone();
            node.setLocalScale(Utils.getRandomNumberInRange(2, 4), Utils.getRandomNumberInRange(2, 4), Utils.getRandomNumberInRange(2, 5));
            return node;
        }

        @Override
        public int getProbability() {
            return probability;
        }
    };

    private static AssetManager assetManager;

    public static void setAssetManager(AssetManager assetManager) {
        TreeTypeEnum.assetManager = assetManager;
    }

    public static Node getRandomTree() {
        Node node = null;

        if(Utils.getRandom(FIR.getProbability())) {
            node = FIR.getNode();
        } else if (Utils.getRandom(OAK.getProbability())) {
            node = OAK.getNode();
        } else if (Utils.getRandom(BOULDER.getProbability())) {
            node = BOULDER.getNode();
        } else if (Utils.getRandom(MAPPLE.getProbability())) {
            node = MAPPLE.getNode();
        } else if (Utils.getRandom(TREE3.getProbability())) {
            node = TREE3.getNode();
        } else if (Utils.getRandom(TREE.getProbability())) {
            node = TREE.getNode();
        } else {
            node = TREE2.getNode();
        }

        return node;
    }
}
