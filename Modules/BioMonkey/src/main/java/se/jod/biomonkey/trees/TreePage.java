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

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import java.util.ArrayList;
import se.jod.biomonkey.paging.DetailLevel;
import se.jod.biomonkey.paging.GeometryPage;
import se.jod.biomonkey.paging.interfaces.Block;
import se.jod.biomonkey.paging.interfaces.PagingManager;

/**
 * This is the default page-type for the treeloader.
 *
 * @author Andreas
 */
public class TreePage extends GeometryPage {

    public TreePage() {
        super();
    }

    public TreePage(int x, int z, PagingManager engine) {
        super(x, z, engine);
    }

    @Override
    public void process(Vector3f camPos) {

        for (Block b : blocks) {
            // If there are no nodes, continue to the next block.
            // TODO this is impossible, remove.
            if (b.getNodes() == null) {
                continue;
            }

            // If the node is created, but there is no geometry, continue.
            // TODO This is what happens if there is no geometry.
            if (b.getNode(0).getChildren().isEmpty()) {
                continue;
            }

            boolean impostors = false;

            // If there are two detail levels impostors are enabled.
            ArrayList<DetailLevel> levels = manager.getDetailLevels();
            if (levels.size() == 2) {
                impostors = true;
            }

            DetailLevel geomL = levels.get(0);

            // ---------- No impostors -------------

            // Get the distance to the page center.
            float dx = b.getCenterPoint().x - camPos.x;
            float dz = b.getCenterPoint().z - camPos.z;
            float dist = FastMath.sqrt(dx * dx + dz * dz);

            // Quick check if no fading.
            if (impostors == false) {
                if (dist < geomL.getFarDist()) {
                    // Visible - but no fading.
                    b.setVisible(true, 0);
                } else {
                    b.setVisible(false, 0);
                }
            }

            // ------------ Impostors ------------
            if (impostors == true) {

                // This is the radius of the (smallest) circle enclosing
                // the page and all its geometry in the xz plane.
                float pageRadius = b.getRealMax();
                float pageMin = dist - pageRadius;
                float pageMax = dist + pageRadius;

                boolean geomVis = false;
                boolean impVis = false;

                DetailLevel impostorL = levels.get(1);

                // If page is completely within far-dist of the geometry, don't blend between
                // levels of detail.
                if (pageMax <= geomL.getFarDist()) {
                    geomVis = true;
                    // If page is somewhere in the transition range, show both geometries and fade
                    // between them.
                } else if (pageMin < geomL.getFarTransDist()) {
                    geomVis = true;
                    impVis = true;
                } else if (pageMin <= impostorL.getFarDist()) {
                    impVis = true;
                }
                // Set visibility for geometry and impostors (LOD 0 and 1).
                b.setVisible(geomVis, 0);
                b.setVisible(impVis, 1);
            } // Impostor block

        }// Page loop
    }// Process method

    @Override
    public TreeBlock createBlock(int x, int y, Vector3f center, PagingManager engine) {
        return new TreeBlock(x, y, center, engine, this);
    }
    
}
