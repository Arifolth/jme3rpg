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

package ru.arifolth.anjrpg.filters;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.material.Material;
import com.jme3.system.AppSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.arifolth.anjrpg.interfaces.Constants;
import ru.arifolth.anjrpg.interfaces.ResolutionPreset;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless tests for the FSR 1 preset math, the settings persistence keys
 * and the FSR material definitions (specification section 5, step 8 and
 * section 11.4 - the parts verifiable without a GL context).
 *
 * <p>Run with {@code mvn -pl GameClient test}.</p>
 */
public class ResolutionPresetTest {

    // ===== Section 5, step 8: preset computation returns strictly smaller
    // internal resolutions for 1080p, 1440p, 2048x1152 and 4K displays =====

    private static final int[][] DISPLAY_SIZES = {
            {1920, 1080}, {2560, 1440}, {2048, 1152}, {3840, 2160}
    };

    @Test
    @DisplayName("every preset is strictly smaller than the display on all common display sizes")
    void testAllPresetsValidOnCommonDisplays() {
        for (int[] display : DISPLAY_SIZES) {
            for (ResolutionPreset preset : ResolutionPreset.values()) {
                assertTrue(preset.isValidFor(display[0], display[1]),
                        preset + " must be valid at " + display[0] + "x" + display[1]);
                assertTrue(preset.computeWidth(display[0]) < display[0],
                        preset + " internal width must be < display width");
                assertTrue(preset.computeHeight(display[1]) < display[1],
                        preset + " internal height must be < display height");
                assertTrue(preset.computeWidth(display[0]) > 0 && preset.computeHeight(display[1]) > 0);
            }
        }
    }

    @Test
    @DisplayName("4K preset table: Ultra Quality 2957x1663, Quality 2573x1447, Balanced 2266x1274, Performance 1920x1080")
    void test4KPresetMath() {
        assertEquals(2957, ResolutionPreset.ULTRA_QUALITY.computeWidth(3840));
        assertEquals(1663, ResolutionPreset.ULTRA_QUALITY.computeHeight(2160));
        assertEquals(2573, ResolutionPreset.QUALITY.computeWidth(3840));
        assertEquals(1447, ResolutionPreset.QUALITY.computeHeight(2160));
        assertEquals(2266, ResolutionPreset.BALANCED.computeWidth(3840));
        assertEquals(1274, ResolutionPreset.BALANCED.computeHeight(2160));
        assertEquals(1920, ResolutionPreset.PERFORMANCE.computeWidth(3840));
        assertEquals(1080, ResolutionPreset.PERFORMANCE.computeHeight(2160));
    }

    @Test
    @DisplayName("1080p preset math matches AMD's FSR 1 tier table within spec rounding")
    void test1080pPresetMath() {
        // AMD's documented 1080p internal sizes: UQ 1477x831 (ratio 0.769),
        // Quality 1280x720 (2/3), Balanced 1133x637, Performance 960x540.
        // The specification pins the ratios to 0.77/0.67/0.59/0.50, whose
        // round() results deviate by at most a pixel or two.
        assertEquals(1478, ResolutionPreset.ULTRA_QUALITY.computeWidth(1920));
        assertEquals(1286, ResolutionPreset.QUALITY.computeWidth(1920));
        assertEquals(1133, ResolutionPreset.BALANCED.computeWidth(1920));
        assertEquals(960, ResolutionPreset.PERFORMANCE.computeWidth(1920));
    }

    @Test
    @DisplayName("2K and ultrawide displays: presets are ratios of the CURRENT resolution, not 4K absolutes")
    void test2KAndUltrawidePresetMath() {
        // 2K (2560x1440): the upscaling target is the current screen, so
        // the internal sizes are ratios of 2560x1440 - never of 3840x2160.
        assertEquals(1971, ResolutionPreset.ULTRA_QUALITY.computeWidth(2560)); // round(2560*.77)
        assertEquals(1109, ResolutionPreset.ULTRA_QUALITY.computeHeight(1440)); // round(1440*.77)
        assertEquals(1715, ResolutionPreset.QUALITY.computeWidth(2560));
        assertEquals(965, ResolutionPreset.QUALITY.computeHeight(1440));
        assertEquals(1510, ResolutionPreset.BALANCED.computeWidth(2560));
        assertEquals(850, ResolutionPreset.BALANCED.computeHeight(1440));
        assertEquals(1280, ResolutionPreset.PERFORMANCE.computeWidth(2560));
        assertEquals(720, ResolutionPreset.PERFORMANCE.computeHeight(1440));
        assertEquals("Quality - 1715x965 -> 2560x1440",
                ResolutionPreset.QUALITY.describe(2560, 1440),
                "describe() reports the CURRENT display size, whatever it is");

        // Ultrawide 3440x1440: every preset strictly smaller on both axes.
        for (ResolutionPreset preset : ResolutionPreset.values()) {
            assertTrue(preset.isValidFor(3440, 1440),
                    preset + " must be valid on an ultrawide display");
            assertTrue(preset.computeWidth(3440) < 3440 && preset.computeHeight(1440) < 1440);
        }
        assertEquals(1720, ResolutionPreset.PERFORMANCE.computeWidth(3440));

        // Odd in-between window sizes (e.g. 1680x1050, 1920x1200): valid too.
        assertTrue(ResolutionPreset.QUALITY.isValidFor(1680, 1050));
        assertTrue(ResolutionPreset.QUALITY.isValidFor(1920, 1200));
        assertTrue(ResolutionPreset.QUALITY.isValidFor(3072, 1728));
    }

    @Test
    @DisplayName("describe() shows preset, computed internal size and display size")
    void testDescribe() {
        String described = ResolutionPreset.QUALITY.describe(3840, 2160);
        assertEquals("Quality - 2573x1447 -> 3840x2160", described);
        // Invalid preset reports unavailability instead of a bogus size.
        assertTrue(ResolutionPreset.ULTRA_QUALITY.describe(2, 2).contains("not available"));
    }

    // ===== Spec 11.2 rule 3: strict validation + fallback =====

    @Test
    @DisplayName("degenerate 1x1 display: no preset valid, smallestValidFor returns null")
    void testNoValidPresetOnDegenerateDisplay() {
        assertNull(ResolutionPreset.smallestValidFor(1, 1));
        for (ResolutionPreset preset : ResolutionPreset.values()) {
            assertFalse(preset.isValidFor(1, 1), preset + " must be invalid at 1x1");
        }
        assertFalse(ResolutionPreset.QUALITY.isValidFor(0, 0));
        assertFalse(ResolutionPreset.QUALITY.isValidFor(-100, 500));
    }

    @Test
    @DisplayName("2x2 display: Ultra Quality invalid, Performance is the smallest valid fallback")
    void testFallbackToSmallestValidPreset() {
        // round(2 * 0.77) = 2, not < 2 -> invalid. round(2 * 0.5) = 1 < 2 -> valid.
        assertFalse(ResolutionPreset.ULTRA_QUALITY.isValidFor(2, 2));
        assertTrue(ResolutionPreset.QUALITY.isValidFor(2, 2));
        assertEquals(ResolutionPreset.PERFORMANCE, ResolutionPreset.smallestValidFor(2, 2));
    }

    // ===== Spec 11.2 rule 6: persistence round trip =====

    @Test
    @DisplayName("preset settings key round-trips through AppSettings; unknown values fall back")
    void testPresetPersistence() {
        AppSettings settings = new AppSettings(false);
        // Absent key -> default Quality
        assertEquals(ResolutionPreset.QUALITY,
                ResolutionPreset.fromName((String) settings.get(Constants.ADVANCED_VIDEO_UPSCALING_PRESET),
                        ResolutionPreset.QUALITY));
        assertEquals(ResolutionPreset.QUALITY, ResolutionPreset.fromName(null, ResolutionPreset.QUALITY));
        assertEquals(ResolutionPreset.QUALITY, ResolutionPreset.fromName("garbage", ResolutionPreset.QUALITY));
        assertEquals(ResolutionPreset.BALANCED, ResolutionPreset.fromName("balanced", ResolutionPreset.QUALITY));

        settings.put(Constants.ADVANCED_VIDEO_UPSCALING_PRESET, ResolutionPreset.ULTRA_QUALITY.name());
        assertEquals(ResolutionPreset.ULTRA_QUALITY,
                ResolutionPreset.fromName((String) settings.get(Constants.ADVANCED_VIDEO_UPSCALING_PRESET),
                        ResolutionPreset.QUALITY));
    }

    @Test
    @DisplayName("upscaling toggle defaults to OFF (absent key) unlike the default-on filter toggles")
    void testToggleDefaults() {
        AppSettings settings = new AppSettings(false);
        // default-on behavior (existing switches)
        assertTrue(ru.arifolth.anjrpg.menu.SettingsUtils.readToggle(settings, Constants.ADVANCED_VIDEO_FILM_GRAIN));
        assertTrue(ru.arifolth.anjrpg.menu.SettingsUtils.readToggle(settings, Constants.ADVANCED_VIDEO_FSR_CAS));
        // default-off behavior (the new upscaling switch)
        assertFalse(ru.arifolth.anjrpg.menu.SettingsUtils.readToggle(settings, Constants.ADVANCED_VIDEO_UPSCALING, false));
        settings.putBoolean(Constants.ADVANCED_VIDEO_UPSCALING, true);
        assertTrue(ru.arifolth.anjrpg.menu.SettingsUtils.readToggle(settings, Constants.ADVANCED_VIDEO_UPSCALING, false));
    }

    // ===== Spec section 5 step 7: shader/material integration (headless
    // part: the material definitions parse and the shader files exist) =====

    @Test
    @DisplayName("FSR EASU, FSR CAS and FSR composite material definitions load and the shaders exist on the assets classpath")
    void testFsrMaterialDefinitions() {
        AssetManager assetManager = new DesktopAssetManager(true);
        assetManager.registerLocator("assets", ClasspathLocator.class);

        Material easu = new Material(assetManager, "Common/MatDefs/Post/FSREASUFilter.j3md");
        assertNotNull(easu, "FSREASUFilter.j3md must parse");
        // Setting the declared uniforms must not throw (verifies the
        // parameter names used by FSREASUFilter.initFilter).
        easu.setVector2("InputSize", new com.jme3.math.Vector2f(2560, 1440));
        easu.setVector2("OutputSize", new com.jme3.math.Vector2f(3840, 2160));
        easu.setInt("NumSamples", 1);

        Material cas = new Material(assetManager, "Common/MatDefs/Post/FSRCASFilter.j3md");
        assertNotNull(cas, "FSRCASFilter.j3md must parse");
        cas.setVector2("Resolution", new com.jme3.math.Vector2f(3840, 2160));
        cas.setFloat("Sharpness", 0.5f);

        Material composite = new Material(assetManager, "Common/MatDefs/Post/FSRCompositeFilter.j3md");
        assertNotNull(composite, "FSRCompositeFilter.j3md must parse");
        composite.setInt("NumSamples", 1);

        // The shader sources referenced by the material definitions exist.
        assertShaderExists(assetManager, "Common/MatDefs/Post/fsr_easu.frag");
        assertShaderExists(assetManager, "Common/MatDefs/Post/fsr_easu.vert");
        assertShaderExists(assetManager, "Common/MatDefs/Post/fsr_cas.frag");
        assertShaderExists(assetManager, "Common/MatDefs/Post/fsr_composite.frag");
        assertShaderExists(assetManager, "Common/MatDefs/Post/fsr_composite.vert");
    }

    private void assertShaderExists(AssetManager assetManager, String path) {
        AssetInfo info = assetManager.locateAsset(new AssetKey<>(path));
        assertNotNull(info, path + " must exist on the assets classpath");
    }
}
