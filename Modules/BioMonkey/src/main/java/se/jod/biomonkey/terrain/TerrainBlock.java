/*
 * Copyright (c) 2012, Andreas Olofsson
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
package se.jod.biomonkey.terrain;

import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.terrain.geomipmap.TerrainQuad;
import se.jod.biomonkey.EcoManager;
import se.jod.biomonkey.paging.BlockListener;
import se.jod.biomonkey.paging.GeometryBlock;
import se.jod.biomonkey.paging.interfaces.PagingManager;

/**
 * Block type for terrains.
 * @author Andreas
 */
public class TerrainBlock extends GeometryBlock {

    protected RigidBodyControl control;
    
    public TerrainBlock() {
    }
    
    public TerrainBlock(int x, int z, Vector3f center, PagingManager manager, TerrainPage page) {
        super(x, z, center, manager, page);
    }
    
    @Override
    public void setNodes(Node[] nodes) {
        this.nodes = nodes;
        for (int i = 0; i < nodes.length; i++) {
            nodes[i].setLocalTranslation(bounds.getCenter());
        }
        stateVec = new boolean[nodes.length];
        RigidBodyControl c = nodes[0].getControl(RigidBodyControl.class);
        if(c != null){
            control = c;
            nodes[0].removeControl(c);
        }
    }

    @Override
    public void setVisible(boolean visible, int detailLevel) {
        if (visible == true && stateVec[detailLevel] == false) {
            parentNode.attachChild(nodes[detailLevel]);
            stateVec[detailLevel] = visible;
            if(control != null){
//                nodes[0].addControl(control);
                control.setEnabled(true);
                control.setPhysicsLocation(nodes[0].getWorldTranslation());
                EcoManager.getInstance().getPhysicsSpace().add(control);
            }
            for (BlockListener listener : listeners) {
                listener.nodesAttached(this);
            }

        } else if (visible == false && stateVec[detailLevel] == true) {
            nodes[detailLevel].removeFromParent();
            stateVec[detailLevel] = visible;
            if(control != null){
//                nodes[0].removeControl(control);
                control.setEnabled(false);
                EcoManager.getInstance().getPhysicsSpace().remove(control);
            }
            for (BlockListener listener : listeners) {
                listener.nodesDetached(this);
            }
        }
    }

    @Override
    public void unload() {
        if (nodes != null) {
            setVisible(false, 0);
            TerrainQuad tq = (TerrainQuad) nodes[0];
            EcoManager.getInstance().getAtmosphereManager().getFogManager().removeMaterial(tq.getMaterial());
            nodes = null;
            stateVec = null;
        }
        
    }
    
}
