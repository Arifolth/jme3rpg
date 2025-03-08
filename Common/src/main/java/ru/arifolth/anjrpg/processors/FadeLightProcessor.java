/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2014 - 2025 Alexander Nilov
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.arifolth.anjrpg.processors;

import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import ru.arifolth.anjrpg.interfaces.ProcessorInterface;

public class FadeLightProcessor implements ProcessorInterface {
    private AmbientLight ambientLight;
    private DirectionalLight sunLight;
    private FadeGammaProcessor fadeGammaProcessor;

    private float fadeOut;

    public FadeLightProcessor(AmbientLight ambientLight, DirectionalLight sunLight, FadeGammaProcessor fadeGammaProcessor) {
        this.ambientLight = ambientLight;
        this.sunLight = sunLight;
        this.fadeGammaProcessor = fadeGammaProcessor;
    }

    @Override
    public void process(float tpf) {
        try {
            if (tpf < 0) {
                if (fadeOut <= -1.0f) {
                    ambientLight.setColor(new ColorRGBA(0.25f, 0.25f, 0.25f, 1.0f));
                    return;
                }
            } else {
                if (fadeOut >= 0.5f) {
                    ambientLight.setColor(new ColorRGBA(0.35f, 0.35f, 0.35f, 1.0f));
                    return;
                }
            }
            fadeOut += tpf / 32;
            sunLight.setColor(ColorRGBA.White.mult(fadeOut));
        } finally {
            fadeGammaProcessor.process(tpf);
        }
    }
}
