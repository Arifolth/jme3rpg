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

package ru.arifolth.anjrpg;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.math.Vector3f;
import com.jme3.post.Filter;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.post.filters.FXAAFilter;
import com.jme3.post.filters.GammaCorrectionFilter;
import com.jme3.post.filters.TranslucentBucketFilter;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.renderer.Camera;
import com.jme3.renderer.Caps;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.water.WaterFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.arifolth.anjrpg.events.EventBus;
import ru.arifolth.anjrpg.filters.DisplayResolutionFogFilter;
import ru.arifolth.anjrpg.filters.FSRCompositeFilter;
import ru.arifolth.anjrpg.filters.FSREASUFilter;
import ru.arifolth.anjrpg.filters.FSRCASFilter;
import ru.arifolth.anjrpg.filters.FilmGrainFilter;
import ru.arifolth.anjrpg.interfaces.ANJRpgInterface;
import ru.arifolth.anjrpg.interfaces.Constants;
import ru.arifolth.anjrpg.interfaces.FilterManagerInterface;
import ru.arifolth.anjrpg.interfaces.FractalTerrainGridInterface;
import ru.arifolth.anjrpg.interfaces.GameLogicCoreInterface;
import ru.arifolth.anjrpg.interfaces.InitStateEnum;
import ru.arifolth.anjrpg.interfaces.LoadingProgressSink;
import ru.arifolth.anjrpg.interfaces.ResolutionPreset;
import ru.arifolth.anjrpg.interfaces.SkyInterface;
import ru.arifolth.anjrpg.interfaces.SoundManagerInterface;
import ru.arifolth.anjrpg.interfaces.TerrainManagerInterface;
import ru.arifolth.anjrpg.interfaces.ThrottledSceneGraphQueueInterface;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless integration tests for the FSR 1 upscaling wiring in
 * {@link FilterManager}: the filter chain order (EASU immediately before
 * RCAS, the terminal FSR composite, the runtime fog inserted before it),
 * the preset -> camera-viewport-rect mechanism, runtime toggling,
 * display-resolution changes and the fallback rules (specification
 * sections 5, 6 and 11.4 - everything verifiable without a GL context;
 * the actual buffer sizes and the visual output need a running renderer
 * and are covered by the architecture documentation instead).
 *
 * <p>The FilterManager is driven with an unstarted SimpleApplication stub
 * (its "context" is a dynamic proxy returning a mutable AppSettings, which
 * is exactly what the real LWJGL3 context exposes), a real headless
 * DesktopAssetManager, a real ViewPort/Camera and dynamic proxies for the
 * Renderer and TerrainManagerInterface.</p>
 *
 * <p>Run with {@code mvn -pl GameClient test}.</p>
 */
public class FilterManagerUpscalingTest {

    private static final int DISPLAY_W = 3840;
    private static final int DISPLAY_H = 2160;

    /** The FilterManager together with the viewport it was built from. */
    private static final class FmHandle {
        final FilterManager filterManager;
        final Camera camera;

        FmHandle(FilterManager filterManager, Camera camera) {
            this.filterManager = filterManager;
            this.camera = camera;
        }
    }

    // =====================================================================
    // Stubs
    // =====================================================================

    /**
     * An unstarted SimpleApplication whose getContext() proxy serves a
     * MUTABLE AppSettings - mirroring the real LWJGL3 context, whose
     * settings track window/framebuffer resizes (unlike the
     * application-level settings).
     */
    private static class TestApp extends SimpleApplication implements ANJRpgInterface {
        final AppSettings appSettings = new AppSettings(false);
        final AppSettings contextSettings = new AppSettings(false);

        TestApp() {
            this(DISPLAY_W, DISPLAY_H);
        }

        /**
         * The display resolution this simulated context runs at - 4K by
         * default, or any other current screen resolution (2K, ultrawide,
         * windowed sizes) for the any-resolution upscaling tests.
         */
        TestApp(int width, int height) {
            contextSettings.setResolution(width, height);
        }

        @Override
        public void simpleInitApp() {
        }

        @Override
        public JmeContext getContext() {
            return proxy(JmeContext.class, (p, method, args) ->
                    "getSettings".equals(method.getName()) ? contextSettings : defaultValue(method.getReturnType()));
        }

        /**
         * A detached RenderManager: enough for the FilterManager to create
         * the present viewport and processor (nothing ever renders in the
         * headless tests; processors initialize lazily on first render).
         */
        @Override
        public com.jme3.renderer.RenderManager getRenderManager() {
            if (super.getRenderManager() == null) {
                try {
                    java.lang.reflect.Field f = com.jme3.app.LegacyApplication.class.getDeclaredField("renderManager");
                    f.setAccessible(true);
                    f.set(this, new com.jme3.renderer.RenderManager(newRenderer()));
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException(e);
                }
            }
            return super.getRenderManager();
        }

        @Override
        public void setUpGUI() {
        }

        @Override
        public InitStateEnum getInitStatus() {
            return InitStateEnum.PENDING;
        }

        @Override
        public AppSettings getSettings() {
            return appSettings;
        }

        @Override
        public void onLoadingComplete() {
        }

        @Override
        public GameLogicCoreInterface getGameLogicCore() {
            return null;
        }

        @Override
        public EventBus getEventBus() {
            return null;
        }

        @Override
        public FilterManagerInterface getFilterManager() {
            return null;
        }

        @Override
        public SoundManagerInterface getSoundManager() {
            return null;
        }

        @Override
        public TerrainManagerInterface getTerrainManager() {
            return null;
        }

        @Override
        public String getVersion() {
            return "test";
        }

        @Override
        public ThrottledSceneGraphQueueInterface getThrottledQueue() {
            return null;
        }

        @Override
        public void loadResources() {
        }

        @Override
        public com.jme3.bullet.BulletAppState getBulletAppState() {
            return null;
        }

        @Override
        public void setProgressSink(LoadingProgressSink sink) {
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> iface, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(FilterManagerUpscalingTest.class.getClassLoader(),
                new Class<?>[]{iface}, handler);
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0f;
        if (type == double.class) return 0.0;
        if (type == Collection.class || type == List.class || type == Set.class) return new ArrayList<>();
        return null;
    }

    private static Renderer newRenderer() {
        return proxy(Renderer.class, (p, method, args) -> {
            if ("getCaps".equals(method.getName())) {
                return EnumSet.of(Caps.OpenGL32, Caps.OpenGL40, Caps.PackedFloatColorBuffer,
                        Caps.PackedFloatTexture, Caps.FrameBufferMultisample, Caps.FloatColorBufferRGBA);
            }
            return defaultValue(method.getReturnType());
        });
    }

    private static TerrainManagerInterface newTerrainManager() {
        // getTerrainBuilder() returns a proxy whose getTileCapturer() is
        // null; FilterManager tolerates that (defensive null check).
        return proxy(TerrainManagerInterface.class, (p, method, args) ->
                "getTerrainBuilder".equals(method.getName())
                        ? proxy(FractalTerrainGridInterface.class, (p2, m2, a2) -> null)
                        : defaultValue(method.getReturnType()));
    }

    private static SkyInterface newSky() {
        return new SkyInterface() {
            @Override
            public void initialize() {
            }

            @Override
            public Vector3f getSunDirection() {
                // Fresh vector every call: FilterManager.update() mutates it.
                return new Vector3f(0.4f, 1.0f, 0.25f).normalize();
            }

            @Override
            public int getHours() {
                return 12;
            }

            @Override
            public void update(float tpf) {
            }
        };
    }

    /** Builds a headless FilterManager with the given persisted upscaling settings. */
    private FmHandle newFilterManager(TestApp app, boolean upscaling, ResolutionPreset preset) {
        return newFilterManager(app, upscaling, preset,
                app.contextSettings.getWidth(), app.contextSettings.getHeight());
    }

    /**
     * Same, at an explicit display resolution - the FSR chain must derive
     * every size from whatever the CURRENT screen resolution is, so the
     * same assertions must hold at 2K, ultrawide, windowed sizes, ... as at
     * 4K (no hardcoded target resolution anywhere in the chain).
     */
    private FmHandle newFilterManager(TestApp app, boolean upscaling, ResolutionPreset preset,
                                      int displayWidth, int displayHeight) {
        if (upscaling) {
            app.appSettings.putBoolean(Constants.ADVANCED_VIDEO_UPSCALING, true);
            app.appSettings.put(Constants.ADVANCED_VIDEO_UPSCALING_PRESET, preset.name());
        }
        AssetManager assetManager = new DesktopAssetManager(true);
        assetManager.registerLocator("assets", ClasspathLocator.class);
        Camera camera = new Camera(displayWidth, displayHeight);
        ViewPort viewPort = new ViewPort("TestMain", camera);
        FilterManager fm = new FilterManager(app, assetManager, new Node("root"), viewPort,
                newSky(), newRenderer(), newTerrainManager());
        return new FmHandle(fm, camera);
    }

    private static int indexOf(FilterPostProcessor fpp, Class<? extends Filter> type) {
        List<Filter> filters = fpp.getFilterList();
        for (int i = 0; i < filters.size(); i++) {
            if (type.isInstance(filters.get(i))) {
                return i;
            }
        }
        return -1;
    }

    // =====================================================================
    // Filter chain order (spec sections 5 step 5 and 6)
    // =====================================================================

    @Test
    @DisplayName("chain order: ... FXAA, Gamma, LightScattering, Water, EASU, RCAS, FilmGrain, Composite (fog inserted before the composite)")
    void testFilterChainOrder() {
        TestApp app = new TestApp();
        FmHandle h = newFilterManager(app, true, ResolutionPreset.QUALITY);
        h.filterManager.initialize();
        FilterPostProcessor fpp = h.filterManager.getFilterPostProcessor();

        // All pre-upscale effects keep their relative order.
        int ssao = indexOf(fpp, SSAOFilter.class);
        int bloom = indexOf(fpp, BloomFilter.class);
        int fxaa = indexOf(fpp, FXAAFilter.class);
        int gamma = indexOf(fpp, GammaCorrectionFilter.class);
        int translucent = indexOf(fpp, TranslucentBucketFilter.class);
        assertTrue(ssao >= 0 && bloom > ssao && translucent > ssao && fxaa > bloom && gamma > fxaa);

        // Water and light scattering run BEFORE the upscaler (stock filters
        // render at the internal resolution; explicitly allowed placement).
        int water = indexOf(fpp, WaterFilter.class);
        int easu = indexOf(fpp, FSREASUFilter.class);
        int cas = indexOf(fpp, FSRCASFilter.class);
        int grain = indexOf(fpp, FilmGrainFilter.class);
        int composite = indexOf(fpp, FSRCompositeFilter.class);
        assertTrue(water >= 0 && water < easu, "Water must run before EASU");
        assertTrue(fxaa < easu, "FXAA must run before EASU (on the low-res image)");
        assertEquals(easu + 1, cas, "RCAS must run IMMEDIATELY after EASU, no filter in between");
        assertTrue(cas < grain, "film grain must run after RCAS");

        // While upscaling is on the composite filter is the TERMINAL filter
        // of the MAIN chain: its material is what the dedicated PRESENT
        // viewport draws over the whole screen (the stock FPP confines the
        // main chain's own final pass to the internal-size sub-rect).
        assertTrue(composite > grain, "the composite goes after the film grain");
        assertEquals(fpp.getFilterList().size() - 1, composite,
                "the composite must be the LAST filter while upscaling is on");

        // The PRESENT viewport: a second main view with a display-sized
        // camera and a FULL rect, carrying the single full-screen quad
        // that re-renders the composite's material.
        ViewPort presentViewPort = h.filterManager.getPresentViewPort();
        assertNotNull(presentViewPort, "present viewport created (test app provides a RenderManager)");
        assertEquals("FSR Present", presentViewPort.getName());
        assertTrue(presentViewPort.isEnabled(), "present viewport active while upscaling is on");
        assertEquals(DISPLAY_W, presentViewPort.getCamera().getWidth());
        assertEquals(DISPLAY_H, presentViewPort.getCamera().getHeight());
        assertEquals(0f, presentViewPort.getCamera().getViewPortLeft(), 1e-6f);
        assertEquals(1f, presentViewPort.getCamera().getViewPortRight(), 1e-6f,
                "the present camera has a FULL rect - no multiView on the present pipeline");
        assertEquals(1, presentViewPort.getScenes().size(),
                "the present viewport renders exactly one scene (the quad node)");
        assertEquals(h.filterManager.getPresentQuad(),
                ((com.jme3.scene.Node) presentViewPort.getScenes().get(0)).getChild(0),
                "the present scene contains the full-screen quad");

        // The runtime fog is INSERTED before the composite (never appended
        // after it) so the composite's material carries the FOGGED image.
        // Note the list grows by one, so the composite index is re-read
        // after addFog().
        h.filterManager.addFog();
        int fog = indexOf(fpp, DisplayResolutionFogFilter.class);
        composite = indexOf(fpp, FSRCompositeFilter.class);
        assertTrue(fog > grain, "fog composites over the grained, upscaled image");
        assertTrue(fog < composite, "fog must be inserted BEFORE the composite");
        assertEquals(fpp.getFilterList().size() - 1, composite,
                "the composite stays the terminal filter after addFog()");
        assertEquals(composite - 1, fog, "fog sits immediately before the composite");
    }

    // =====================================================================
    // Upscaling off by default (spec 11.2: absent key = native path)
    // =====================================================================

    @Test
    @DisplayName("absent settings keys: upscaling off, full camera rect, EASU present but disabled")
    void testUpscalingDisabledByDefault() {
        TestApp app = new TestApp();
        FmHandle h = newFilterManager(app, false, null);
        h.filterManager.initialize();

        assertFalse(h.filterManager.isUpscalingEnabled());
        assertEquals(DISPLAY_W, h.filterManager.getInternalWidth());
        assertEquals(DISPLAY_H, h.filterManager.getInternalHeight());
        assertEquals(0f, h.camera.getViewPortLeft(), 1e-6f);
        assertEquals(1f, h.camera.getViewPortRight(), 1e-6f);
        assertEquals(0f, h.camera.getViewPortBottom(), 1e-6f);
        assertEquals(1f, h.camera.getViewPortTop(), 1e-6f);
        assertNotNull(h.filterManager.getFsrEASUFilter());
        assertNotNull(h.filterManager.getFsrCASFilter());
        assertNotNull(h.filterManager.getFilmGrainFilter());
        assertFalse(h.filterManager.getFsrEASUFilter().isEnabled(),
                "EASU must be disabled while upscaling is off");
        assertTrue(h.filterManager.getFsrCASFilter().isEnabled(),
                "CAS keeps its own default-on toggle");

        // On the native path the main FPP's own final pass covers the full
        // screen (full saved rect), so neither the composite filter nor
        // the PRESENT viewport belongs in the render path.
        FilterPostProcessor fpp = h.filterManager.getFilterPostProcessor();
        assertEquals(-1, indexOf(fpp, FSRCompositeFilter.class),
                "the composite filter must not be in the chain while upscaling is off");
        assertFalse(h.filterManager.getPresentViewPort().isEnabled(),
                "present viewport disabled while upscaling is off");

        // On the native path the fog simply appends (no composite to step
        // over) and is the terminal filter - exactly the stock behaviour.
        h.filterManager.addFog();
        assertEquals(fpp.getFilterList().size() - 1, indexOf(fpp, DisplayResolutionFogFilter.class),
                "without the composite the fog appends last (native path)");
    }

    // =====================================================================
    // Preset application (spec 5 step 6 and 11.2 rule 1)
    // =====================================================================

    @Test
    @DisplayName("Quality preset at 4K: internal 2573x1447, camera rect ~0.67, EASU enabled")
    void testQualityPresetAt4K() {
        TestApp app = new TestApp();
        FmHandle h = newFilterManager(app, true, ResolutionPreset.QUALITY);
        h.filterManager.initialize();

        assertTrue(h.filterManager.isUpscalingEnabled());
        assertEquals(ResolutionPreset.QUALITY, h.filterManager.getUpscalingPreset());
        assertEquals(2573, h.filterManager.getInternalWidth(), "round(3840 * 0.67)");
        assertEquals(1447, h.filterManager.getInternalHeight(), "round(2160 * 0.67)");

        // The camera rect is the internal/display ratio, nudged by half a
        // texel so the FPP's truncating (int)(canvas * rect) lands exactly
        // on the internal size.
        float expectedRectX = (2573 + 0.5f) / (float) DISPLAY_W;
        float expectedRectY = (1447 + 0.5f) / (float) DISPLAY_H;
        assertEquals(expectedRectX, h.camera.getViewPortRight(), 1e-6f);
        assertEquals(expectedRectY, h.camera.getViewPortTop(), 1e-6f);
        assertEquals(0f, h.camera.getViewPortLeft(), 1e-6f);
        assertEquals(0f, h.camera.getViewPortBottom(), 1e-6f);

        // The FPP derives its buffer width from the same rect (identical
        // truncation) - i.e. the scene buffer matches the internal size.
        assertEquals(2573, (int) (DISPLAY_W * expectedRectX));
        assertEquals(1447, (int) (DISPLAY_H * expectedRectY));

        assertNotNull(h.filterManager.getFsrEASUFilter());
        assertTrue(h.filterManager.getFsrEASUFilter().isEnabled());
        assertTrue(h.filterManager.getFsrCASFilter().isEnabled());
        // Regression: RCAS must never be configured to 0.0 sharpness - the
        // rewritten fsr_cas.frag maps 0.0 to an exact pass-through, so a
        // zero value silently disables the whole sharpening stage (the
        // "no visible FSR CAS effect" bug was exactly that plus the old
        // shader's no-op math).
        assertTrue(h.filterManager.getFsrCASFilter().getSharpness() > 0f,
                "RCAS sharpness must be > 0 (0.0 = pass-through = off)");
        assertEquals(0.8f, h.filterManager.getFsrCASFilter().getSharpness(), 0f);
    }

    // =====================================================================
    // Any-resolution upscaling: the chain derives every size from the
    // CURRENT display resolution - 2K, ultrawide or windowed sizes take the
    // exact same code path as 4K (nothing in the chain may hardcode a
    // target resolution; the presets are ratios of whatever the screen is)
    // =====================================================================

    @Test
    @DisplayName("Quality preset at a 2K display: EASU upscales 1715x965 -> 2560x1440, silently")
    void testQualityPresetAt2KDisplay() {
        TestApp app = new TestApp(2560, 1440);
        FmHandle h = newFilterManager(app, true, ResolutionPreset.QUALITY, 2560, 1440);
        h.filterManager.initialize();

        assertTrue(h.filterManager.isUpscalingEnabled());
        // round(2560 * 0.67) = 1715, round(1440 * 0.67) = 965
        assertEquals(1715, h.filterManager.getInternalWidth(), "round(2560 * 0.67)");
        assertEquals(965, h.filterManager.getInternalHeight(), "round(1440 * 0.67)");

        // Same camera-rect mechanism as at 4K, just relative to the 2K size.
        assertEquals((1715 + 0.5f) / 2560f, h.camera.getViewPortRight(), 1e-6f);
        assertEquals((965 + 0.5f) / 1440f, h.camera.getViewPortTop(), 1e-6f);
        assertEquals(1715, (int) (2560 * ((1715 + 0.5f) / 2560f)), "FPP derivation matches");

        // The EASU stage must target the CURRENT display resolution...
        FSREASUFilter easuFilter = (FSREASUFilter) h.filterManager.getFsrEASUFilter();
        assertEquals(1715, easuFilter.getInputWidth(), "EASU input = internal 2K size");
        assertEquals(965, easuFilter.getInputHeight(), "EASU input = internal 2K size");
        assertEquals(2560, easuFilter.getOutputWidth(), "EASU output = the current display width");
        assertEquals(1440, easuFilter.getOutputHeight(), "EASU output = the current display height");

        // ...and the presentation viewport presents at exactly that size.
        assertEquals(2560, h.filterManager.getPresentViewPort().getCamera().getWidth(),
                "present viewport camera at the 2K display width");
        assertEquals(1440, h.filterManager.getPresentViewPort().getCamera().getHeight(),
                "present viewport camera at the 2K display height");
        assertTrue(h.filterManager.getPresentViewPort().isEnabled());
    }

    @Test
    @DisplayName("ultrawide 3440x1440 display: Performance preset upscales 1720x720 -> 3440x1440")
    void testUltrawideDisplay() {
        TestApp app = new TestApp(3440, 1440);
        FmHandle h = newFilterManager(app, true, ResolutionPreset.PERFORMANCE, 3440, 1440);
        h.filterManager.initialize();

        assertEquals(1720, h.filterManager.getInternalWidth(), "round(3440 * 0.50)");
        assertEquals(720, h.filterManager.getInternalHeight(), "round(1440 * 0.50)");
        FSREASUFilter easu = (FSREASUFilter) h.filterManager.getFsrEASUFilter();
        assertEquals(1720, easu.getInputWidth());
        assertEquals(3440, easu.getOutputWidth(), "EASU upscales to the ultrawide width");
        assertEquals(1440, easu.getOutputHeight());
        assertEquals(3440, h.filterManager.getPresentViewPort().getCamera().getWidth());
    }

    @Test
    @DisplayName("small windowed 1600x900 display: upscaling still valid and correctly sized")
    void testSmallWindowedDisplay() {
        TestApp app = new TestApp(1600, 900);
        FmHandle h = newFilterManager(app, true, ResolutionPreset.BALANCED, 1600, 900);
        h.filterManager.initialize();

        // round(1600 * 0.59) = 944, round(900 * 0.59) = 531
        assertEquals(944, h.filterManager.getInternalWidth());
        assertEquals(531, h.filterManager.getInternalHeight());
        assertTrue(h.filterManager.isUpscalingEnabled(),
                "ratio presets stay valid at windowed sizes - no fallback needed");
        FSREASUFilter easu = (FSREASUFilter) h.filterManager.getFsrEASUFilter();
        assertEquals(944, easu.getInputWidth());
        assertEquals(1600, easu.getOutputWidth());
        assertEquals(900, easu.getOutputHeight());
    }

    @Test
    @DisplayName("runtime preset change recomputes the internal size and the camera rect")
    void testRuntimePresetChange() {
        TestApp app = new TestApp();
        FmHandle h = newFilterManager(app, true, ResolutionPreset.QUALITY);
        h.filterManager.initialize();
        assertEquals(2573, h.filterManager.getInternalWidth());

        h.filterManager.applyResolutionPreset(ResolutionPreset.PERFORMANCE);
        assertEquals(ResolutionPreset.PERFORMANCE, h.filterManager.getUpscalingPreset());
        assertEquals(1920, h.filterManager.getInternalWidth());
        assertEquals(1080, h.filterManager.getInternalHeight());
        float expectedRectX = (1920 + 0.5f) / (float) DISPLAY_W;
        assertEquals(expectedRectX, h.camera.getViewPortRight(), 1e-6f);

        // Storing a preset while upscaling is OFF keeps the value but has no
        // effect on the internal size (spec 11.2, rule 4).
        h.filterManager.setUpscalingEnabled(false);
        assertEquals(DISPLAY_W, h.filterManager.getInternalWidth());
        assertEquals(1f, h.camera.getViewPortRight(), 1e-6f);
        h.filterManager.applyResolutionPreset(ResolutionPreset.BALANCED);
        assertEquals(DISPLAY_W, h.filterManager.getInternalWidth(), "preset has no effect while upscaling is off");
        assertEquals(ResolutionPreset.BALANCED, h.filterManager.getUpscalingPreset(), "but the selection is retained");

        // Re-enabling applies the retained preset (spec 11.2, rule 4).
        h.filterManager.setUpscalingEnabled(true);
        assertEquals(2266, h.filterManager.getInternalWidth());
        assertTrue(h.filterManager.getFsrEASUFilter().isEnabled());
    }

    // =====================================================================
    // Terminal composite filter (the full-screen presentation pass)
    // =====================================================================

    @Test
    @DisplayName("composite lifecycle: in the main chain only while upscaling is on; present viewport follows")
    void testCompositeFilterLifecycle() {
        TestApp app = new TestApp();
        FmHandle h = newFilterManager(app, true, ResolutionPreset.PERFORMANCE);
        FilterPostProcessor fpp = h.filterManager.getFilterPostProcessor();

        // Before initialize() the composite filter does not exist yet, but
        // the present viewport is already active (it clears the screen and
        // renders its fallback quad behind the loading screen).
        assertTrue(h.filterManager.getPresentViewPort().isEnabled(),
                "present viewport active from the start (clears the screen during loading)");
        assertEquals(-1, indexOf(fpp, FSRCompositeFilter.class),
                "no composite before initialize()");

        h.filterManager.initialize();
        assertEquals(fpp.getFilterList().size() - 1, indexOf(fpp, FSRCompositeFilter.class),
                "composite is the terminal main filter once the chain exists");

        // Runtime toggle OFF removes it (native path needs no composite)
        // and idles the present viewport.
        h.filterManager.setUpscalingEnabled(false);
        assertEquals(-1, indexOf(fpp, FSRCompositeFilter.class),
                "composite must leave the chain when upscaling is turned off");
        assertFalse(h.filterManager.getPresentViewPort().isEnabled(),
                "present viewport disabled while upscaling is off");

        // Runtime toggle ON re-appends it at the terminal position and
        // re-activates the present viewport.
        h.filterManager.setUpscalingEnabled(true);
        assertEquals(fpp.getFilterList().size() - 1, indexOf(fpp, FSRCompositeFilter.class),
                "composite returns as the terminal filter");
        assertTrue(h.filterManager.getPresentViewPort().isEnabled(),
                "present viewport re-enabled");

        // removeFog keeps the composite terminal; addFog re-inserts the fog
        // before it (covered by the chain-order test) - and removing the fog
        // again must not disturb the composite's position.
        h.filterManager.addFog();
        h.filterManager.removeFog();
        assertEquals(fpp.getFilterList().size() - 1, indexOf(fpp, FSRCompositeFilter.class),
                "composite stays terminal across fog add/remove");
        assertEquals(-1, indexOf(fpp, DisplayResolutionFogFilter.class), "fog is gone");
    }

    // =====================================================================
    // Runtime toggle (spec 11.1 table: Enable Upscaling)
    // =====================================================================

    @Test
    @DisplayName("runtime toggle off/on restores the native rect and re-applies the preset")
    void testRuntimeToggle() {
        TestApp app = new TestApp();
        FmHandle h = newFilterManager(app, true, ResolutionPreset.ULTRA_QUALITY);
        h.filterManager.initialize();
        assertEquals(2957, h.filterManager.getInternalWidth());

        h.filterManager.setUpscalingEnabled(false);
        assertFalse(h.filterManager.isUpscalingEnabled());
        assertEquals(DISPLAY_W, h.filterManager.getInternalWidth());
        assertEquals(DISPLAY_H, h.filterManager.getInternalHeight());
        assertEquals(1f, h.camera.getViewPortRight(), 1e-6f);
        assertFalse(h.filterManager.getFsrEASUFilter().isEnabled(), "EASU must be bypassed while upscaling is off");

        h.filterManager.setUpscalingEnabled(true);
        assertTrue(h.filterManager.isUpscalingEnabled());
        assertEquals(2957, h.filterManager.getInternalWidth());
        assertTrue(h.filterManager.getFsrEASUFilter().isEnabled());
    }

    @Test
    @DisplayName("CAS and grain toggles compose with upscaling (independent switches)")
    void testCasAndGrainTogglesWithUpscaling() {
        TestApp app = new TestApp();
        FmHandle h = newFilterManager(app, true, ResolutionPreset.QUALITY);
        h.filterManager.initialize();

        // CAS off while upscaling active: EASU stays on, grain still last.
        h.filterManager.getFsrCASFilter().setEnabled(false);
        assertFalse(h.filterManager.getFsrCASFilter().isEnabled());
        assertTrue(h.filterManager.getFsrEASUFilter().isEnabled());

        // Grain off: order untouched.
        h.filterManager.getFilmGrainFilter().setEnabled(false);
        assertFalse(h.filterManager.getFilmGrainFilter().isEnabled());
        assertEquals(indexOf(h.filterManager.getFilterPostProcessor(), FSREASUFilter.class) + 1,
                indexOf(h.filterManager.getFilterPostProcessor(), FSRCASFilter.class));

        // Upscaling off with CAS/grain still off: everything consistent.
        h.filterManager.setUpscalingEnabled(false);
        assertFalse(h.filterManager.getFsrEASUFilter().isEnabled());
        assertEquals(DISPLAY_W, h.filterManager.getInternalWidth());
    }

    // =====================================================================
    // Display-resolution change (spec 11.2 rules 2 and 3)
    // =====================================================================

    @Test
    @DisplayName("display resize recomputes the internal resolution for the current preset")
    void testDisplayResolutionChange() {
        TestApp app = new TestApp();
        FmHandle h = newFilterManager(app, true, ResolutionPreset.QUALITY);
        h.filterManager.initialize();
        assertEquals(2573, h.filterManager.getInternalWidth());

        // Simulate the DisplayResolutionTracker notification (what the engine
        // delivers on window resize / fullscreen toggle): the context
        // settings carry the new size, then onDisplayResolution runs BEFORE
        // the FPP rebuilds its buffers from the saved rect.
        app.contextSettings.setResolution(2560, 1440);
        h.filterManager.onDisplayResolution(2560, 1440);

        // Internal size recomputed with the SAME truncation the FPP applies
        // to the saved rect fractions, so EASU's InputSize matches the real
        // buffer exactly.
        float savedRectX = (2573 + 0.5f) / (float) DISPLAY_W;
        float savedRectY = (1447 + 0.5f) / (float) DISPLAY_H;
        int expectedW = Math.max(1, (int) (2560 * savedRectX));
        int expectedH = Math.max(1, (int) (1440 * savedRectY));
        assertEquals(expectedW, h.filterManager.getInternalWidth());
        assertEquals(expectedH, h.filterManager.getInternalHeight());
        assertTrue(h.filterManager.isUpscalingEnabled());
        assertTrue(h.filterManager.getFsrEASUFilter().isEnabled());

        // 1080p window: still upscaling, still valid (ratio presets adapt).
        app.contextSettings.setResolution(1920, 1080);
        h.filterManager.onDisplayResolution(1920, 1080);
        assertTrue(h.filterManager.isUpscalingEnabled());
        assertTrue(h.filterManager.getInternalWidth() < 1920 && h.filterManager.getInternalHeight() < 1080);
    }

    @Test
    @DisplayName("degenerate display disables upscaling; invalid preset falls back to Performance")
    void testFallbackRules() {
        TestApp app = new TestApp();
        FmHandle h = newFilterManager(app, true, ResolutionPreset.QUALITY);
        h.filterManager.initialize();

        // Degenerate 1x1 display: no preset valid -> upscaling disabled
        // (spec 11.2, rule 3), native rect restored.
        app.contextSettings.setResolution(1, 1);
        h.filterManager.onDisplayResolution(1, 1);
        assertFalse(h.filterManager.isUpscalingEnabled());
        assertFalse(h.filterManager.getFsrEASUFilter().isEnabled());
        assertEquals(1f, h.camera.getViewPortRight(), 1e-6f);

        // Ultra Quality on a 2x2 display: invalid (round(2*0.77) == 2),
        // falls back to the smallest valid preset (Performance).
        TestApp tinyApp = new TestApp();
        tinyApp.contextSettings.setResolution(2, 2);
        FmHandle tiny = newFilterManager(tinyApp, true, ResolutionPreset.ULTRA_QUALITY);
        // The constructor itself validates: smallest-valid fallback applies.
        assertEquals(ResolutionPreset.PERFORMANCE, tiny.filterManager.getUpscalingPreset());
        assertTrue(tiny.filterManager.isUpscalingEnabled());
        assertTrue(tiny.filterManager.getInternalWidth() < 2 && tiny.filterManager.getInternalHeight() < 2);
    }

    @Test
    @DisplayName("runtime preset change on an invalid preset falls back to the smallest valid one")
    void testRuntimeInvalidPresetFallsBack() {
        TestApp app = new TestApp();
        FmHandle h = newFilterManager(app, true, ResolutionPreset.QUALITY);
        h.filterManager.initialize();

        // Shrink the display to 2x2 via the tracker, then pick Ultra Quality:
        // invalid at 2x2 -> falls back to Performance (spec 11.2, rule 3).
        app.contextSettings.setResolution(2, 2);
        h.filterManager.onDisplayResolution(2, 2);
        h.filterManager.applyResolutionPreset(ResolutionPreset.ULTRA_QUALITY);
        assertEquals(ResolutionPreset.PERFORMANCE, h.filterManager.getUpscalingPreset());
        assertEquals(1, h.filterManager.getInternalWidth());
        assertEquals(1, h.filterManager.getInternalHeight());
        assertTrue(h.filterManager.isUpscalingEnabled());
    }

    // =====================================================================
    // Settings restore (spec 11.2 rule 6)
    // =====================================================================

    @Test
    @DisplayName("persisted settings are honored at startup")
    void testPersistedSettingsRestore() {
        TestApp app = new TestApp();
        app.appSettings.putBoolean(Constants.ADVANCED_VIDEO_UPSCALING, true);
        app.appSettings.put(Constants.ADVANCED_VIDEO_UPSCALING_PRESET, ResolutionPreset.BALANCED.name());
        FmHandle h = newFilterManager(app, false, null); // ctor flags off: keys only
        h.filterManager.initialize();

        assertTrue(h.filterManager.isUpscalingEnabled());
        assertEquals(ResolutionPreset.BALANCED, h.filterManager.getUpscalingPreset());
        assertEquals(2266, h.filterManager.getInternalWidth());
        assertEquals(1274, h.filterManager.getInternalHeight());
    }
}
