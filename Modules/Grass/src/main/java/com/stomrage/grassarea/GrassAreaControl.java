/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stomrage.grassarea;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A control class to check for the Closest GrassBlade chunk everyframe
 * @author Stomrage
 */
public class GrassAreaControl implements Control {

    //A grass comparator to sort the Grass by distance to the camera
    private GrassComparator grassComp;
    private Camera cam;
    private GrassArea grassArea;

    /**
     * An empty construct @see Savable
     */
    public GrassAreaControl() {
    }

    /**
     * The GrassAreaControl construct, only take the camera since the GrassArea will
     * be linked with the setSpatial control method
     * @param cam Your main scene camera
     */
    public GrassAreaControl(Camera cam) {
        this.cam = cam;
        grassComp = new GrassComparator(cam);
    }

    /**
     * Compare the x,z distance of 2 3d vector
     * @param camLocation The camera location (adjusted or not)
     * @param location The location of the GrassObject to comapre
     * @return 
     */
    private float compareRealDistance(Vector3f camLocation, Vector3f location) {
        Vector2f cam2D = new Vector2f(camLocation.x, camLocation.z);
        Vector2f loc2D = new Vector2f(location.x, location.z);
        return cam2D.distance(loc2D);
    }

    public Control cloneForSpatial(Spatial spatial) {
        GrassAreaControl control = null;
        if (spatial instanceof GrassArea) {
            control = new GrassAreaControl(cam);
        }
        return control;
    }

    public void setSpatial(Spatial spatial) {
        if (spatial instanceof GrassArea) {
            this.grassArea = (GrassArea) spatial;
        } else {
            System.err.println("Erreur le spatial doit etre une GrassArea");
        }
    }

    public void update(float tpf) {
        Vector3f adjustedCam = cam.getLocation().clone();
        adjustedCam.addLocal(cam.getDirection().x * GrassFactory.getInstance().getGrassDist(), 0, cam.getDirection().z * GrassFactory.getInstance().getGrassDist());
        List<GrassBlade> grassBlades = new ArrayList<GrassBlade>();
        GrassObject[][] grassPatches = grassArea.getPatch();
        int division = grassArea.getDivision()-1;
        for (int x = 0; x < 2; x++) {
            for (int z = 0; z < 2; z++) {
                if (compareRealDistance(adjustedCam, grassPatches[x][z].getLocation()) < (grassArea.getSize()/2)*division+GrassFactory.getInstance().getGrassDist()) {
                    checkForHolder((GrassPatch) grassPatches[x][z], grassBlades, division - 1, adjustedCam);
                }
            }
        }
        Collections.sort(grassBlades, grassComp);
        grassArea.createMesh(grassBlades);
    }

    /**
     * This method is here to check the GrassArea tree system and compare them
     * with the level of subdivision, until a GrassHolder is found and then
     * fill the GrassBlades list in parameter
     * @param g The current grass object
     * @param grassBlades The list of GrassBlade to draw
     * @param division The current subdivion level
     * @param adjustedCam The position of the camera
     */
    private void checkForHolder(GrassPatch g, List<GrassBlade> grassBlades, int division, Vector3f adjustedCam) {
        GrassObject[][] grassPatches = g.getPatch();
        for (int x = 0; x < 2; x++) {
            for (int z = 0; z < 2; z++) {
                if (grassPatches[x][z] instanceof GrassPatch) {
                    GrassPatch patch = (GrassPatch) grassPatches[x][z];
                    if (compareRealDistance(adjustedCam, grassPatches[x][z].getLocation()) < (patch.getSize()/2)*division+GrassFactory.getInstance().getGrassDist()) {
                        checkForHolder(patch, grassBlades, division - 1, adjustedCam);
                    }
                } else {
                    GrassHolder holder = (GrassHolder) grassPatches[x][z];
                    if (compareRealDistance(adjustedCam, holder.getLocation()) < (holder.getSize()/2)*division+GrassFactory.getInstance().getGrassDist()) {
                        grassBlades.addAll(holder.getGrass());
                    }
                }
            }
        }
    }

    public void render(RenderManager rm, ViewPort vp) {
        //Nothing to do
    }

    public void write(JmeExporter ex) throws IOException {
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(grassArea, "grassArea", new GrassArea());
    }

    public void read(JmeImporter im) throws IOException {
        InputCapsule capsule = im.getCapsule(this);
        this.grassArea = (GrassArea) capsule.readSavable("grassArea", new GrassArea());
        grassComp = null;
        cam = null;
    }

    public void setCamera(Camera cam) {
        this.cam = cam;
        grassComp = new GrassComparator(cam);
    }

    public class GrassComparator implements Comparator<GrassBlade> {

        private Camera cam;

        public GrassComparator(Camera cam) {
            this.cam = cam;
        }

        public int compare(GrassBlade o1, GrassBlade o2) {
            float dist1 = compareRealDistance(cam.getLocation(), o1.position);
            float dist2 = compareRealDistance(cam.getLocation(), o2.position);
            if (dist1 > dist2) {
                return -1;
            }
            if (dist1 == dist2) {
                return 0;
            }
            return 1;
        }
    }
}
