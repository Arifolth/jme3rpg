/*
 * Copyright (c) 2011, Andreas Olofsson
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions 
 * are met:
 * 
 * Redistributions of source code must retain the above copyright notice, 
 * this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright notice, 
 * this list of conditions and the following disclaimer in the documentation 
 * and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED 
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package se.jod.biomonkey.trees;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import java.io.IOException;
import se.jod.biomonkey.EcoManager;
import se.jod.biomonkey.paging.GeometryBlock;
import se.jod.biomonkey.paging.interfaces.PagingManager;

/**
 * This is the default page type for the tree-loader.
 *
 * @author Andreas
 */
public class TreeBlock extends GeometryBlock {

    protected RigidBodyControl control;
    protected boolean debug = false;
    protected Node[] debugShapes;

    public TreeBlock() {
    }
    
    public TreeBlock(int x, int z, Vector3f center, PagingManager engine, TreePage page) {
        super(x, z, center, engine, page);
        if (engine.isDebug()) {
            debug = true;
            debugShapes = new Node[2];
            debugShapes[0] = new Node("TreeGeomDebug");
            debugShapes[1] = new Node("TreeImpostorDebug");
        }
    }

    @Override
    public void setVisible(boolean visible, int detailLevel) {

        if (visible == true && stateVec[detailLevel] == false) {
            parentNode.attachChild(nodes[detailLevel]);
            stateVec[detailLevel] = true;

            if (debug == true) {
                parentNode.attachChild(debugShapes[detailLevel]);
            }

            if (detailLevel == 0 && control != null) {
                PhysicsSpace phySpace = EcoManager.getInstance().getPhysicsSpace();
                phySpace.add(control);
                control.setPhysicsLocation(bounds.getCenter());
            }
        } else if (visible == false && stateVec[detailLevel] == true) {
            nodes[detailLevel].removeFromParent();
            stateVec[detailLevel] = false;

            if (debug == true) {
                debugShapes[detailLevel].removeFromParent();
            }

            if (detailLevel == 0 && control != null) {
                PhysicsSpace phySpace = EcoManager.getInstance().getPhysicsSpace();
                phySpace.remove(control);
            }
        }
    }

    /**
     * Create a rigid body control and initialize everything.
     * @param ccs 
     */
    public void initPhysics(CompoundCollisionShape ccs) {

        if (!nodes[0].getChildren().isEmpty()) {

            if (ccs != null && !ccs.getChildren().isEmpty()) {
                control = new RigidBodyControl(ccs, 0f);
                nodes[0].addControl(control);
                control.setPhysicsLocation(parentNode.getWorldTranslation());
            }
        }
    }

    /**
     * Serialization use only.
     */
    public void initPhysics() {
        if (control != null) {
            nodes[0].addControl(control);
            control.setPhysicsLocation(parentNode.getWorldTranslation());
        }
    }

    @Override
    public void unload() {
        super.unload();
        if (control != null) {
            PhysicsSpace phySpace = EcoManager.getInstance().getPhysicsSpace();
            phySpace.remove(control);
        }
    }
    
//    @Override
//    public void setNodes(Node[] nodes) {
//        super.setNodes(nodes);
//        if(debug){
//            for(int i = 0; i < nodes.length; i++){
//                BoundingBox bb = (BoundingBox) nodes[i].getWorldBound();
//                if(bb == null){
//                    bb = new BoundingBox();
//                }
//                Box box = new Box(bb.getCenter().clone().subtractLocal(bounds.getCenter()),bb.getXExtent(),bb.getYExtent(),bb.getZExtent());
//                Geometry geom = new Geometry("Debug" + i,box);
//                Material mat = new Material(EcoManager.getInstance().getApp().getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
//                ColorRGBA col = null;
//                if(i == 0){
//                    col = ColorRGBA.Red;
//                    col.a = 0.3f;
//                } else {
//                    col = ColorRGBA.Blue;
//                    col.a = 0.3f;
//                    geom.setLocalScale(0.95f);
//                }
//                
//                mat.setColor("Color", col);
//                geom.setMaterial(mat);
//                debugShapes[i].attachChild(geom);
//                debugShapes[i].setLocalTranslation(bounds.getCenter());
//            }
//        }
//    }
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        control = (RigidBodyControl) capsule.readSavable("physicsControl", null);
        // Remember init physics
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(control, "physicsControl", null);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TreeBlock other = (TreeBlock) obj;
        if (other.hash != this.hash) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return "TreePage (" + Short.toString(x) + ','
                + Short.toString(z) + ')';
    }
    
}//TreePage
