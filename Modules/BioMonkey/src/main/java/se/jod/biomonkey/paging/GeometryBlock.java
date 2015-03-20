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
package se.jod.biomonkey.paging;

import com.jme3.bounding.BoundingBox;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import se.jod.biomonkey.RectBounds;
import se.jod.biomonkey.paging.grid.GenericCell2D;
import se.jod.biomonkey.paging.interfaces.Block;
import se.jod.biomonkey.paging.interfaces.Page;
import se.jod.biomonkey.paging.interfaces.PagingManager;

/**
 * Base class for Blocks.
 *
 * @author Andreas
 */
public class GeometryBlock extends GenericCell2D implements Block, Savable {

    protected transient List<BlockListener> listeners;
    protected transient Page page;
    protected transient Node parentNode;
    protected transient boolean[] stateVec;
    protected Node[] nodes;
    protected float realMax;
    protected RectBounds bounds;

    /* Serialization constructor. */
    public GeometryBlock() {
        super();
    }

    /**
     * Constructor based on x and z coordinates.
     *
     * @param x The x-coordinate of the block.
     * @param z The z-coordinate of the block.
     * @param center The center of the block.
     * @param manager The paging manager used for this blocktype.
     */
    public GeometryBlock(int x, int z, Vector3f center, PagingManager manager, GeometryPage page) {
        super(x, z);
        listeners = new ArrayList<BlockListener>();
        this.parentNode = manager.getPagingNode();
        this.page = page;
        bounds = new RectBounds(center, manager.getBlockSize());
    }

    @Override
    public void setNodes(Node[] nodes) {
        this.nodes = nodes;
        for (int i = 0; i < nodes.length; i++) {
            nodes[i].setLocalTranslation(bounds.getCenter());
            calculateRealMax(i);
        }
        stateVec = new boolean[nodes.length];
        
    }

    @Override
    public Node[] getNodes() {
        return nodes;
    }

    @Override
    public Node getNode(int detailLevel) {
        return nodes[detailLevel];
    }

    @Override
    public boolean isVisible(int detailLevel) {
        return stateVec[detailLevel];
    }

    @Override
    public void unload() {
        if (nodes != null) {
            for (int i = 0; i < nodes.length; i++) {
                setVisible(false, i);
            }
            nodes = null;
            stateVec = null;
        }
    }

    @Override
    public Vector3f getCenterPoint() {
        return bounds.getCenter();
    }

    @Override
    public RectBounds getBounds() {
        return bounds;
    }

    public void addBlockListener(BlockListener listener) {
        this.listeners.add(listener);
    }

    @Override
    public float getRealMax() {
        return realMax;
    }

    @Override
    public void setRealMax(float realMax) {
        this.realMax = realMax;
    }

    @Override
    public void calculateRealMax(int detailLevel) {

        Node node = nodes[detailLevel];
        // Nodes should not have interacted with the scenegraph at this point.
        node.updateModelBound();

        float ol = bounds.getWidth() * 0.70711f; // Half blocksize * sqrt(2).

        if (node.getWorldBound() == null) {
            realMax = ol;
            return;
        }

        BoundingBox wb = (BoundingBox) node.getWorldBound();

        // The difference between bounding volume and block centers.
        // These values needs to be added to the real max.
        float dX = FastMath.abs(wb.getCenter().x - bounds.getCenter().x);
        float dZ = FastMath.abs(wb.getCenter().z - bounds.getCenter().z);

        BoundingBox bb = (BoundingBox) wb;

        float temp = (dX + bb.getXExtent()) * 1.4142f; //sqrt(2)
        if (temp > ol) {
            ol = temp;
        }

        temp = (dZ + bb.getZExtent()) * 1.4142f;
        if (temp > ol) {
            ol = temp;
        }

        realMax = ol;
    }

    @Override
    public void update(float tpf) {
    }

    @Override
    public void setParentNode(Node parentNode) {
        this.parentNode = parentNode;
    }

    @Override
    public Node getParentNode() {
        return parentNode;
    }

    @Override
    public Page getPage() {
        return page;
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        bounds = (RectBounds) capsule.readSavable("bounds", null);
        realMax = capsule.readFloat("realMax", 0);
        Savable[] nds = capsule.readSavableArray("nodes", null);
        Node[] nodez = new Node[nds.length];
        for (int i = 0; i < nds.length; i++) {
            nodez[i] = (Node) nds[i];
        }
        // We want to do all the stuff in setNodes(), not just add
        // them.
        setNodes(nodez);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(nodes, "nodes", null);
        capsule.write(realMax, "realMax", 0);
        capsule.write(bounds, "bounds", null);
    }

    @Override
    public void setVisible(boolean visible, int detailLevel) {
    }
}
