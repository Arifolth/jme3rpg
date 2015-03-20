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

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import se.jod.biomonkey.paging.DetailLevel;
import se.jod.biomonkey.paging.GeometryPage;
import se.jod.biomonkey.paging.interfaces.Block;
import se.jod.biomonkey.paging.interfaces.PagingManager;


/**
 * Pagetype used for grass.
 * 
 * @author Andreas
 */
public class GrassPage extends GeometryPage {
    
    public GrassPage(){
        super();
    }
    
    public GrassPage(int x, int z, PagingManager manager) {
        super(x, z, manager);
    }
    
    @Override
    public void process(Vector3f camPos) {
        
        for (Block b : blocks) {
            // If there are no nodes, continue to the next block.
            // TODO this is impossible, remove.
            if(b.getNodes() == null){
                continue;
            }
            
            // If the node is created, but there is no geometry, continue.
            // TODO This is what happens if there is no geometry.
            if(b.getNode(0).getChildren().isEmpty()){
                continue;
            }
            
            DetailLevel grassL = manager.getDetailLevels().get(0);
            boolean vis = false;
            
            boolean fading = false;
            if(grassL.isFadeEnabled() == true){
                fading = true;
            }
            // ---------- No fading -------------
            
            //Get the distance to the page center.
            float dx = b.getCenterPoint().x - camPos.x;
            float dz = b.getCenterPoint().z - camPos.z;
            float dist = FastMath.sqrt(dx * dx + dz * dz);
            
            // Quick check if no fading.
            if(fading == false){
                if (dist < grassL.getFarDist()) {
                    vis = true;
                }
                b.setVisible(vis, 0);
                continue;
            }
            
            // ------------ Impostors ------------
            if(fading == true){
                
                //This is the radius of the (smallest) circle enclosing
                //the page and all its geometry in the xz plane.
                float pageRadius = b.getRealMax();
                float pageMin = dist - pageRadius;
                float pageMax = dist + pageRadius;
                
                //Standard visibility check.
                if (pageMax < grassL.getFarDist()) {
                    vis = true;
                } else if (pageMax >= grassL.getFarDist() && pageMin < grassL.getFarTransDist()){
                    vis = true;
                }
                b.setVisible(vis, 0);
            } // Impostor block
            
        }//Page loop
    }//Process method

    @Override
    public GrassBlock createBlock(int x, int y, Vector3f center, PagingManager manager) {
        return new GrassBlock(x,y,center,manager,this);
    }
    
    @Override
    public String toString() {
        return "GrassPage(" + Short.toString(x) + ',' + Short.toString(z) + ')';
    }
    
}//GrassPage
