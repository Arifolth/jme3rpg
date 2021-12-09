/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stomrage.grassarea;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import java.io.IOException;

/**
 * A wrapper for the String array list
 * @author Stomrage
 * @version 0.1
 */
public class SavableString implements Savable {

    public String string;

    public SavableString() {
    }

    public SavableString(String s) {
        this.string = s;
    }

    public void write(JmeExporter ex) throws IOException {
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(string, "string", "");
    }

    public void read(JmeImporter im) throws IOException {
        InputCapsule capsule = im.getCapsule(this);
        string = capsule.readString("string", "");
    }
}
