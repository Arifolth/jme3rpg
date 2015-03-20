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
package se.jod.biomonkey.paging.interfaces;

import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import se.jod.biomonkey.RectBounds;

/**
 * This is the blocks interface. Blocks could be seen as the leaves of the paging 
 * tree. Normally, all scenegraph contents are stored in blocks, whereas information
 * related to the paging process is stored within the (higher-level) page objects.
 * 
 * @author Andreas
 */
public interface Block{
    
    /**
     * This method is called to update the block. It has to be implemented but
     * doesn't have to do anything.
     * 
     * @param tpf The number of seconds passed since the last frame. 
     */
    public void update(float tpf);
    
    /**
     * This method is called when the parent page (and thus the block) is removed
     * from the paging grid.
     */
    public void unload();
    
    /**
     * Get the centerpoint of the block.
     * 
     * @return The centerpoint.
     */
    public Vector3f getCenterPoint();
    
    /**
     * Get the bounds object of the block.
     * @return 
     */
    public RectBounds getBounds();
    
    /**
     * Sets the nodes of the geometry block.
     * 
     * @param nodes The nodes to be set. 
     */
    public void setNodes(Node[] nodes);

    /**
     * Get the nodes of the geometry block.
     * @return 
     */
    public Node[] getNodes();
    
    /**
     * Gets the node corresponding to the given level of detail.
     * 
     * @param detailLevel The level of detail.
     * @return The node.
     */
    public Node getNode(int detailLevel);
    
    /**
     * Set the parent node (paging node). This is handled automatically.
     * @param parentNode 
     */
    public void setParentNode(Node parentNode);
    
    /**
     * Get the parent node (paging node).
     * @return 
     */
    public Node getParentNode();
    
    /**
     * Gets the visibility status of a node.
     * 
     * @param detailLevel The detail level index. Detail levels are used to index the nodes.
     * @return The visibility status of the node.
     */
    public boolean isVisible(int detailLevel);
    
    /**
     * Changes the visibility status of a node.
     * 
     * @param visible true or false.
     * @param detailLevel The detail level index.
     */
    public void setVisible(boolean visible, int detailLevel);
    
    public float getRealMax();
    
    /**
     * This method is used to find out how large the block actually is 
     * (in the xz-plane). The real maximum value is the radius of the 
     * smallest possible circle centered at the block center, that covers 
     * the block and all of the geometry inside the block.
     * </br>
     * This value defaults to blockSize / sqrt(2), which is the
     * distance from the block center to either of its corners. If the bounding
     * box of the geometry inside the block extends outside of the block bounds,
     * the real max is extended.
     * 
     * @param detailLevel The level of detail (node index).
     */
    public void calculateRealMax(int detailLevel);
    
    /**
     * Use to set the real max value (see calculateRealMax) if it's
     * known in advance. Like if using a terrain quad, which always
     * has a realmax of pageSize/sqrt(2)
     * @param realMax 
     */
    public void setRealMax(float realMax);
    
    /**
     * Get the parent page object.
     * 
     * @return 
     */
    public Page getPage();
    
}//Page
