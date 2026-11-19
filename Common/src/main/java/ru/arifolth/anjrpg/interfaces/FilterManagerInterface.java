/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2014 - 2026 Alexander Nilov
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

package ru.arifolth.anjrpg.interfaces;

import com.jme3.post.Filter;
import com.jme3.post.filters.GammaCorrectionFilter;

public interface FilterManagerInterface {
    void initialize();

    GammaCorrectionFilter getGammaCorrectionFilter();

    /**
     * The film grain filter. It is deliberately added LAST to the
     * FilterPostProcessor chain (after Water) so the grain composites over
     * the final image. Runtime toggling must go through
     * {@link Filter#setEnabled(boolean)}, which skips the pass without
     * changing the filter's index - never remove/add, which would move it
     * to the end of the chain and break the order.
     *
     * <p>Declared as the engine base type because the concrete filter lives
     * in GameClient while this interface is in Common, which the Menu
     * module (the toggling UI) compiles against.</p>
     */
    Filter getFilmGrainFilter();

    /**
     * The FSR EASU upscaling filter (FSR 1 spatial upscaler, internal ->
     * display resolution). It is added after the Water filter and
     * IMMEDIATELY before the FSR CAS filter - no filter may run between
     * EASU and RCAS (FSR 1 pipeline requirement). Use
     * {@link #setUpscalingEnabled(boolean)} to toggle it (never remove/add,
     * which would break the chain order).
     *
     * <p>Declared as the engine base type, like getFilmGrainFilter().</p>
     */
    Filter getFsrEASUFilter();

    /**
     * The FSR CAS sharpening filter (RCAS stage of the FSR 1 pipeline). It
     * is added IMMEDIATELY after {@link #getFsrEASUFilter()} and must stay
     * there. Runtime toggling must go through
     * {@link Filter#setEnabled(boolean)} only.
     */
    Filter getFsrCASFilter();

    /**
     * Enables/disables the FSR 1 upscaling path (Advanced Video Settings
     * menu). When enabled, the scene renders at the internal resolution
     * derived from the current preset and the EASU filter upscales it to
     * the display resolution; when disabled, the scene renders natively.
     * Applies immediately at runtime (safe to call while the world is
     * loaded); before the world loads, persist the
     * Constants.ADVANCED_VIDEO_UPSCALING key instead - FilterManager applies
     * it when the filters are created.
     */
    void setUpscalingEnabled(boolean enabled);

    /**
     * Selects the FSR 1 quality preset (relative to the CURRENT display
     * resolution - see {@link ResolutionPreset}). Validates the preset
     * against the display resolution and falls back to the smallest valid
     * preset (or disables upscaling) when it is not valid. Applies
     * immediately when upscaling is enabled; when disabled, the preset is
     * stored without effect until upscaling is turned on.
     */
    void applyResolutionPreset(ResolutionPreset preset);

    /** Whether the FSR upscaling path is currently active. */
    boolean isUpscalingEnabled();

    /** The currently selected FSR upscaling preset (persisted value). */
    ResolutionPreset getUpscalingPreset();

    void addFog();

    void removeFog();

    void update(float tpf);
}
