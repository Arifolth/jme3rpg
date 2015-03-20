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

import com.jme3.scene.Node;
import java.util.concurrent.Callable;

/**
 * <Code>PageLoader</code>s are used by paging engines to load pages.
 * Normally these classes serve as UIs for the paging system as well.
 * 
 * @author Andreas
 */
public interface PageLoader {
    
    /**
     * This method is called to load a page. This method should return null if 
     * the pageloader has no data associated with the page in question.
     * 
     * @param page The page to be loaded.
     * @return A loading-task callable.
     */
    public Callable<Boolean> loadPage(Page page);
    
    /**
     * This is a method for updating the pageloader. Normally the paging manager
     * is updated from within pageloaders.
     * 
     * @param tpf The number of seconds since the last frame.
     */
    public void update(float tpf);
    
    /**
     * This method is used to create pages. Pageloaders are also page factories.
     * 
     * @param x The x-coordinate of the page.
     * @param z The z-coordinate of the page.
     * @return A page of the proper type.
     */
    public Page createPage(int x, int z);
    
    /**
     * Get the paging manager used by this page loader.
     * @return The paging manager.
     */
    public PagingManager getPagingManager();
    
    /**
     * Get the paging node of this pageloader.
     * 
     * @return 
     */
    public Node getPagingNode();
    public String getName();
    public void setName(String name);
    public int getID();
    public void setID(int ID);
    
    /**
     * Adds to the version ID. This should be done whenever
     * an operation cause already loaded pages to become obsolete.
     */
    public void incrementPageVersion();
    public int getPageVersion();
    
}//TileLoader
