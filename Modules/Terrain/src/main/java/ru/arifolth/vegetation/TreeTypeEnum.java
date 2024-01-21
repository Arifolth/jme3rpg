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

import com.jme3.asset.AssetManager;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.util.TangentBinormalGenerator;
import ru.arifolth.anjrpg.interfaces.LodUtils;
import ru.arifolth.anjrpg.interfaces.TreeType;
import ru.arifolth.anjrpg.interfaces.Utils;

public enum TreeTypeEnum implements TreeType {
    FIR {
        Node tree = null;
        final int probability = 80;
        @Override
        public void init() {
            Node firTree = (Node) assetManager.loadModel("Models/Fir1/fir1_androlo.j3o");
            firTree.setShadowMode(RenderQueue.ShadowMode.Cast);

            LodUtils.setUpModelLod(firTree);
            TangentBinormalGenerator.generate(firTree);

            tree = firTree;
        }

        @Override
        public Node getTree() {
            Node node = (Node) tree.clone();
            node.setLocalScale(Utils.getRandomNumberInRange(1, 2), Utils.getRandomNumberInRange(1, 2), Utils.getRandomNumberInRange(1, 3));
            return node;
        }

        @Override
        public int getProbability() {
            return probability;
        }
    },
    MAPPLE {
        Node tree = null;
        final int probability = 10;
        @Override
        public void init() {
            Node mapleTree = (Node) assetManager.loadModel("Models/Maple/tree_maple.j3o");
            Geometry mapleTrunk = (Geometry) mapleTree.getChild("maple trunk");
            mapleTrunk.setMaterial(assetManager.loadMaterial("Models/Maple/Maple_Trunk.j3m"));

            Geometry mapleLeaves = (Geometry) mapleTree.getChild("maple leaves");
            mapleLeaves.setMaterial(assetManager.loadMaterial("Models/Maple/Maple_Leaves.j3m"));


            mapleTree.setShadowMode(RenderQueue.ShadowMode.Cast);
            LodUtils.setUpModelLod(mapleTree);
            TangentBinormalGenerator.generate(mapleTree);


            tree = mapleTree;
        }

        @Override
        public Node getTree() {
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
            LodUtils.setUpModelLod(oakTree);
            TangentBinormalGenerator.generate(oakTree);

            tree = oakTree;
        }

        @Override
        public Node getTree() {
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
        Node tree = null;

        if(Utils.getRandom(FIR.getProbability())) {
            tree = FIR.getTree();
        } else if (Utils.getRandom(OAK.getProbability())) {
            tree = OAK.getTree();
        } else { //other 10%
            tree = MAPPLE.getTree();
        }

        return tree;
    }
}
