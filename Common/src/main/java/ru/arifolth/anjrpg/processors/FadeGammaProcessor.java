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

import com.jme3.post.filters.GammaCorrectionFilter;
import ru.arifolth.anjrpg.interfaces.ProcessorInterface;

public class FadeGammaProcessor  implements ProcessorInterface {
    private GammaCorrectionFilter gammaCorrectionFilter;
    private final static float MAX_GAMMA_CAP = 0.84f;
    private final static float MIN_GAMMA_CAP = 0.6f;
    private float fadeOut = MIN_GAMMA_CAP;

    public FadeGammaProcessor(GammaCorrectionFilter gammaCorrectionFilter) {
        this.gammaCorrectionFilter = gammaCorrectionFilter;
        this.gammaCorrectionFilter.setGamma(MIN_GAMMA_CAP); // Default gamma is 1.0; values >1.0 make the scene darker, <1.0 make it brighter
    }

    @Override
    public void process(float tpf) {
        if(tpf < 0) {
            if(fadeOut <= MIN_GAMMA_CAP) {
                return;
            }
        } else {
            if(fadeOut >= MAX_GAMMA_CAP) {
                return;
            }
        }

        fadeOut += tpf / 16;
        gammaCorrectionFilter.setGamma(fadeOut);
    }
}