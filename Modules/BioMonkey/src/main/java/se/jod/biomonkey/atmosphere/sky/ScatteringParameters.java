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
package se.jod.biomonkey.atmosphere.sky;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.Vector3f;
import java.io.IOException;

/**
 * Contains the parameters used in the scattering shader.
 * 
 * @author Andreas
 */
public class ScatteringParameters implements Savable {
    
    public ScatteringParameters(){}
    
    // Inner atmosphere radius
    protected float InnerRadius = 9.77501f;
    // Outer atmosphere radius
    protected float OuterRadius = 10.2963f;
    // Height position, in [0, 1] range, 0=InnerRadius, 1=OuterRadius
    protected float HeightPosition = 0.01f;
    // Rayleigh multiplier
    protected float RayleighMultiplier = 0.0022f;
    // Mie multiplier
    protected float MieMultiplier = 0.000675f;
    // Sun intensity
    protected float SunIntensity = 30;

    // WaveLength for RGB channels
//    protected Vector3f WaveLength = new Vector3f(0.57f, 0.54f, 0.44f);
    protected Vector3f WaveLength = new Vector3f(0.65f, 0.57f, 0.475f);
    /// Phase function
    protected float G = -0.9991f;

    // Exposure coeficient
    protected float Exposure = 4.0f;

    // Number of samples
    protected int NumberOfSamples = 4;

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(InnerRadius, "InnerRadius", 0);
        capsule.write(OuterRadius, "OuterRadius", 0);
        capsule.write(RayleighMultiplier, "RayleighMultiplier", 0);
        capsule.write(MieMultiplier, "MieMultiplier", 0);
        capsule.write(SunIntensity, "SunIntensity", 0);
        capsule.write(WaveLength, "WaveLength", null);
        capsule.write(G, "G", 0);
        capsule.write(Exposure, "Exposure", 0);
        capsule.write(NumberOfSamples, "NumberOfSamples", 0);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule capsule = im.getCapsule(this);
        InnerRadius = capsule.readFloat("InnerRadius", 0);
        OuterRadius = capsule.readFloat("OuterRadius", 0);
        RayleighMultiplier = capsule.readFloat("RayleighMultiplier", 0);
        MieMultiplier = capsule.readFloat("SunIntensity", 0);
        SunIntensity = capsule.readFloat("SunIntensity", 0);
        WaveLength = (Vector3f) capsule.readSavable("WaveLength", null);
        G = capsule.readFloat("G", 0);
        Exposure = capsule.readFloat("Exposure", 0);
        NumberOfSamples = capsule.readInt("NumberOfSamples", 0);
    }
    
    
}
