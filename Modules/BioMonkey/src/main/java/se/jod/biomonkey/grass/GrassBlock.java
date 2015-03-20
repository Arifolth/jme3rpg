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
package se.jod.biomonkey.grass;

import com.jme3.bounding.BoundingBox;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import java.io.IOException;
import se.jod.biomonkey.EcoManager;
import se.jod.biomonkey.paging.GeometryBlock;
import se.jod.biomonkey.paging.interfaces.PagingManager;

/**
 * Custom page type for grass.
 *
 * @author Andreas
 */
public class GrassBlock extends GeometryBlock {

    protected transient boolean debug = false;
    protected transient Node debugShape;

    public GrassBlock() {
    }

    public GrassBlock(int x, int z, Vector3f center, PagingManager engine, GrassPage page) {
        super(x, z, center, engine, page);
        if (engine.isDebug()) {
            debug = true;
            debugShape = new Node("GrassDebug");
        }
    }

    @Override
    public void setVisible(boolean visible, int detailLevel) {
        if (visible == true && stateVec[detailLevel] == false) {
            parentNode.attachChild(nodes[detailLevel]);
            stateVec[detailLevel] = visible;
            if (debug == true) {
                parentNode.attachChild(debugShape);
            }

        } else if (visible == false && stateVec[detailLevel] == true) {
            nodes[detailLevel].removeFromParent();
            stateVec[detailLevel] = visible;

            if (debug == true) {
                debugShape.removeFromParent();
            }
        }
    }

    @Override
    public void setNodes(Node[] nodes) {
        super.setNodes(nodes);
        if (debug) {
            nodes[0].updateGeometricState();
            BoundingBox bb = (BoundingBox) nodes[0].getWorldBound();
            if (bb == null) {
                bb = new BoundingBox();
            }
            Box box = new Box(bb.getCenter().clone().subtractLocal(bounds.getCenter()), bb.getXExtent(), bb.getYExtent(), bb.getZExtent());
            Geometry geom = new Geometry("Debug", box);
            Material mat = new Material(EcoManager.getInstance().getApp().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            ColorRGBA col;

            col = ColorRGBA.Green;
            col.a = 0.3f;

            mat.setColor("Color", col);
            mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
            geom.setMaterial(mat);
            geom.setQueueBucket(Bucket.Transparent);
            debugShape.attachChild(geom);
            debugShape.setLocalTranslation(bounds.getCenter());
        }        
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GrassBlock other = (GrassBlock) obj;
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
        return "GrassPage (" + Short.toString(x) + ','
                + Short.toString(z) + ')';
    }
}//GrassPage
