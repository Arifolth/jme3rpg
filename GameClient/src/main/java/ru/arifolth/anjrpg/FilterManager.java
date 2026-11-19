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
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.Filter;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.SceneProcessor;
import com.jme3.post.filters.*;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.shadow.PssmShadowRenderer;
import com.jme3.system.AppSettings;
import com.jme3.ui.Picture;
import com.jme3.water.WaterFilter;
import ru.arifolth.anjrpg.filters.DisplayResolutionFogFilter;
import ru.arifolth.anjrpg.filters.FilmGrainFilter;
import ru.arifolth.anjrpg.filters.FSRCompositeFilter;
import ru.arifolth.anjrpg.filters.FSREASUFilter;
import ru.arifolth.anjrpg.filters.FSRCASFilter;
import ru.arifolth.anjrpg.interfaces.*;
import ru.arifolth.anjrpg.menu.SettingsUtils;
import ru.arifolth.terrain.grass.GpuGrassMaterial;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Post-processing chain, extended with the FSR 1 upscaling pipeline (EASU +
 * RCAS) on top of the existing effects.
 *
 * <p><b>How the low-resolution path works.</b> jME's
 * {@link FilterPostProcessor} sizes its scene framebuffer (and every filter's
 * render pass) from the viewport camera's fractional viewport rect: a rect
 * of (0, 0.67, 0, 0.67) makes the FPP render the scene and all filter passes
 * at 67% resolution - the engine's built-in "multiView" support. This manager
 * therefore renders the 3D scene at the internal resolution by shrinking the
 * camera rect according to the selected {@link ResolutionPreset} BEFORE the
 * FPP is attached (the FPP saves the rect at initialization and re-reads it
 * on every canvas reshape, so window resizes recompute the internal
 * resolution automatically). The GUI viewport keeps its own full-size
 * camera, so menus and HUD stay at native resolution.</p>
 *
 * <p><b>How the final image reaches the screen (the PRESENT viewport).</b>
 * The stock FPP's own final pass reuses the saved fractional rect on a
 * display-sized camera, so the main chain's output could only ever land
 * in the internal-size bottom-left sub-rectangle of the screen - the
 * engine has no code path that stretches a small buffer to a bigger
 * screen. The full-screen presentation therefore runs through a SECOND,
 * completely stock pipeline: a dedicated "FSR Present" main viewport
 * (renders after the main view, before the GUI) with a display-resolution
 * camera and a FULL viewport rect, whose scene is a single full-screen
 * quad rendered with the {@link FSRCompositeFilter}'s material. That
 * material is fed by the FPP every frame with the main chain's final
 * display-resolution image (the output of the runtime fog, film grain,
 * RCAS or EASU - whichever filter precedes the terminal composite), so
 * the present viewport draws it over the entire screen through an
 * ordinary scene render: no fractional rect, no multiView, no manual GL
 * state - the same code path that renders every non-upscaled jME scene,
 * which makes the presentation behave identically on every GPU/driver
 * stack. (Revision 3: the earlier design drew the composite manually
 * from the FPP's postFilter hook, which proved unreliable on some
 * drivers.) The present viewport also clears the whole screen every
 * frame while it is enabled, so stale content outside the upscaled image
 * can never remain visible - not even during loading.</p>
 *
 * <p><b>Main filter order</b> (add order == render order):</p>
 * <ol>
 *   <li>SSAO, DepthOfField, TranslucentBucket, CartoonEdge, Bloom, ToneMap,
 *       FXAA, GammaCorrection (constructor) - internal resolution</li>
 *   <li>LightScattering, Water (initialize()) - internal resolution. The
 *       stock WaterFilter cannot run at display resolution in the middle of
 *       the chain, so it runs BEFORE the upscaler; its reflections simply
 *       render at the internal size (the explicitly allowed placement of the
 *       FSR specification, section 6: "If water must be low-res, move it
 *       before EASU").</li>
 *   <li>FSREASU (initialize()) - upscales internal -> display resolution</li>
 *   <li>FSRCAS (initialize()) - RCAS sharpening, IMMEDIATELY after EASU</li>
 *   <li>FilmGrain (initialize()) - after RCAS, at the display resolution</li>
 *   <li>Fog (runtime, addFog()) - INSERTED before the composite (never
 *       appended after it): a DisplayResolutionFogFilter, so it blends
 *       over the upscaled image at the display resolution instead of
 *       downsampling it back to the internal one</li>
 *   <li>FSRComposite (initialize()) - ALWAYS the terminal filter while
 *       upscaling is on: a plain 1:1 copy whose material carries the
 *       final image; the PRESENT viewport renders that material over the
 *       WHOLE screen (the FPP's own final pass would only cover the
 *       internal-size bottom-left sub-rectangle of the screen, the
 *       "image in the bottom-left quarter" bug)</li>
 * </ol>
 *
 * <p><b>FXAA runs before EASU</b> (it anti-aliases the low-res image first), RCAS
 * runs immediately after EASU with no filter in between (FSR 1 pipeline
 * requirement), film grain composites over the final upscaled image, and
 * the FSR composite filter is the terminal pass while upscaling is on (a
 * pure presentation copy with no effect on the image; the fog appended
 * before it is the last effect filter in that case).</p>
 *
 * <p><b>Runtime control.</b> {@link #setUpscalingEnabled(boolean)} and
 * {@link #applyResolutionPreset(ResolutionPreset)} (both wired to the
 * Advanced Video Settings menu) rebuild the FPP via the standard
 * remove/re-add processor dance, which is the only clean way to change the
 * FPP's saved viewport rect at runtime. Display-resolution changes are
 * tracked by the {@link DisplayResolutionTracker} processor, which runs
 * BEFORE the FPP in the viewport's processor list so that the FSR filters
 * are re-initialized with fresh display dimensions on every reshape; it
 * also resizes the present viewport's camera. The internal size is
 * recomputed with the same truncation the FPP uses
 * ({@link #fppDerivedWidth()}/{@link #fppDerivedHeight()}), so the EASU
 * InputSize always matches the actual low-res buffer. Every preset
 * application is logged (preset, internal size, display size, validation
 * result).</p>
 */
public class FilterManager implements FilterManagerInterface {

    private static final Logger LOGGER = Logger.getLogger(FilterManager.class.getName());

    private Node rootNode;
    private AssetManager assetManager;
    private LightScatteringFilter lsf;
    private PssmShadowRenderer pssmRenderer;
    private SkyInterface sky;
    private  ViewPort viewPort;
    private WaterFilter waterFilter;
    private FilterPostProcessor fpp;
    private DisplayResolutionFogFilter fog;
    private GammaCorrectionFilter gammaCorrectionFilter;
    private Renderer renderer;
    private TileTextureCapturer tileTextureCapturer;
    private SimpleApplication application;
    private FilmGrainFilter filmGrainFilter;
    private FSRCASFilter fsrCASFilter;
    private FSREASUFilter fsrEASUFilter;
    private FSRCompositeFilter fsrCompositeFilter;

    // ===== Present-viewport state =====
    /** The dedicated full-screen presentation viewport (null in headless tests). */
    private ViewPort presentViewPort;
    /** The full-screen quad drawn by the present viewport (renders the composite's material). */
    private final Picture presentQuad = new Picture("FSR present quad");
    /** Fallback material for the quad before the composite filter exists (never visible). */
    private Material presentFallbackMaterial;

    // ===== FSR 1 upscaling state =====
    /** Master switch, persisted as Constants.ADVANCED_VIDEO_UPSCALING (default off). */
    private boolean useUpscaling;
    /** Selected quality preset, persisted as Constants.ADVANCED_VIDEO_UPSCALING_PRESET. */
    private ResolutionPreset currentPreset = ResolutionPreset.QUALITY;
    /** Current display (canvas/framebuffer) resolution. */
    private int displayWidth, displayHeight;
    /** Effective internal render resolution (equals display when upscaling is off). */
    private int internalWidth, internalHeight;
    /** Viewport-rect fractions last applied to the camera (mirrors the FPP's saved rect). */
    private float rectX = 1f, rectY = 1f;
    /** Signature of the last applied upscaling configuration, to skip no-op rebuilds. */
    private String appliedUpscalingSignature = "";

    /**
     * Subscribes to display-resolution changes: RenderManager.notifyReshape
     * calls processors' reshape in list order, and this tracker is added
     * BEFORE the FPP, so the FSR filter dimensions are refreshed before the
     * FPP re-initializes its (and our) framebuffers.
     */
    private final DisplayResolutionTracker displayResolutionTracker = new DisplayResolutionTracker();

    public FilterManager(SimpleApplication application, AssetManager assetManager, Node rootNode, ViewPort viewPort, SkyInterface sky, Renderer renderer, TerrainManagerInterface terrainManager) {
        this.application = application;
        this.assetManager = assetManager;
        this.rootNode = rootNode;
        this.viewPort = viewPort;
        this.sky = sky;
        this.renderer = renderer;

        this.renderer.setDefaultAnisotropicFilter(1);

        setupShadowRenderer();
        setupFilterPostProcessor(assetManager);

        setupSSAOFilter();
        setupDepthOfFieldFilter();

        setupFog();
        setupTranslucentBucketFilter();

        setupCartoonEdgeFilter();

        setupBloomFilter();
        setupToneMapFilter();

        setupFXAAFilter();

        // NOTE: FSR CAS (RCAS sharpening) is NOT added here anymore: the FSR 1
        // pipeline requires RCAS to run IMMEDIATELY after the EASU upscaler,
        // which is inserted after the Water filter in initialize(). The full
        // chain order is documented on this class.
        setupGammaCorrectionFilter();
        // NOTE: the film grain filter is deliberately NOT added here.
        // FilterPostProcessor executes filters in the order they were
        // added and the last enabled filter's output goes to the screen,
        // so anything appended later composites OVER the grained image.
        // The WaterFilter is appended later (initialize()), so grain
        // added here ran before the water and the water pass kept
        // repainting clean, ungrained pixels over it - the "water is not
        // affected by film grain" bug. See initialize().

        // Read the persisted upscaling configuration and size the camera
        // viewport rect BEFORE the FPP is attached: the FPP saves the rect at
        // initialization and derives every internal buffer size from it.
        readUpscalingSettings();
        applyUpscalingConfiguration(false);

        viewPort.addProcessor(displayResolutionTracker);
        viewPort.addProcessor(fpp);

        // The full-screen presentation pipeline for the upscaled image:
        // enabled exactly while upscaling is active, drawing whatever the
        // terminal composite filter's material holds (once initialize()
        // has created the main FSR chain).
        setupPresentViewport();

        tileTextureCapturer = terrainManager.getTerrainBuilder().getTileCapturer();
    }

    /**
     * Creates the PRESENT viewport: a second main view (rendered after the
     * main view, before the GUI post view) with a display-resolution
     * camera and a FULL viewport rect, whose scene is a single 1x1
     * full-screen quad (an NDC-mapped {@link Picture}, never resized -
     * the material's vertex shader ignores the camera and the transform,
     * so the quad always covers whatever it is rendered to). The quad's
     * material is swapped to the terminal {@link FSRCompositeFilter}'s
     * material as soon as that exists; until then a fallback material
     * renders black behind the loading screen.
     *
     * <p>A tiny SceneProcessor (the {@link PresentMaterialBinder}) keeps
     * the quad's material current every frame, so any re-initialization
     * of the composite (preset changes, display resizes, the
     * fog re-insertion dance) is picked up automatically. Headless
     * environments without a RenderManager (the test harness) skip the
     * viewport; the quad and binder wiring still exist for tests.</p>
     */
    private void setupPresentViewport() {
        presentQuad.setWidth(1);
        presentQuad.setHeight(1);
        // The regular Scene bucket (not the Picture's default Gui bucket):
        // the quad renders through the ordinary scene path, and its
        // material disables depth testing anyway.
        presentQuad.setQueueBucket(RenderQueue.Bucket.Inherit);
        presentFallbackMaterial = new Material(assetManager, "Common/MatDefs/Post/FSRCompositeFilter.j3md");
        presentQuad.setMaterial(presentFallbackMaterial);

        RenderManager rm = application.getRenderManager();
        if (rm == null) {
            // Headless: no viewport can be attached; the quad/binder wiring
            // is still fully constructed and testable.
            LOGGER.fine("FSR present viewport skipped: no RenderManager (headless)");
            return;
        }

        readDisplayResolution();
        Camera presentCamera = new Camera(displayWidth, displayHeight);
        presentViewPort = rm.createMainView("FSR Present", presentCamera);
        presentViewPort.setClearFlags(true, true, true);
        presentViewPort.setBackgroundColor(ColorRGBA.Black);

        // One static quad, updated once: nothing ever moves it, so no
        // per-frame scene-graph updates are needed (the geometry is
        // rendered through the completely stock viewport path). The
        // present camera never looks AT the quad (the material's vertex
        // shader maps it straight to normalized device coordinates), so
        // both the node and the quad must be exempt from frustum culling -
        // their tiny world bounds at the origin would cull the subtree
        // and blank the presentation.
        Node presentScene = new Node("FSR Present scene");
        presentScene.setCullHint(Spatial.CullHint.Never);
        presentScene.attachChild(presentQuad);
        presentScene.updateGeometricState();
        presentViewPort.attachScene(presentScene);

        presentViewPort.addProcessor(new PresentMaterialBinder());
        presentViewPort.setEnabled(useUpscaling);
    }

    /**
     * Keeps the present viewport's quad rendering the terminal composite
     * filter's material (the FPP refreshes that material's "Texture" with
     * the final chain image every frame). Runs in the present viewport's
     * preFrame, before the quad is rendered.
     */
    private final class PresentMaterialBinder implements SceneProcessor {
        private boolean initialized;

        @Override
        public void initialize(RenderManager rm, ViewPort vp) {
            initialized = true;
        }

        @Override
        public void reshape(ViewPort vp, int w, int h) {
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }

        @Override
        public void preFrame(float tpf) {
            Material live = fsrCompositeFilter != null ? fsrCompositeFilter.getPresentMaterial() : null;
            if (live != null && presentQuad.getMaterial() != live) {
                presentQuad.setMaterial(live);
            }
        }

        @Override
        public void postQueue(RenderQueue rq) {
        }

        @Override
        public void postFrame(com.jme3.texture.FrameBuffer out) {
        }

        @Override
        public void cleanup() {
            initialized = false;
        }

        @Override
        public void setProfiler(AppProfiler profiler) {
        }
    }

    /**
     * Keeps the present viewport active exactly while FSR upscaling is
     * enabled. Called from every upscaling (re-)configuration.
     */
    private void syncPresentViewport() {
        if (presentViewPort != null) {
            presentViewPort.setEnabled(useUpscaling);
        }
    }

    /**
     * Keeps the FSR composite filter in the MAIN chain exactly while FSR
     * upscaling is enabled. While upscaling is on, the composite filter
     * MUST be the terminal filter of the chain (its material is what the
     * present viewport draws full-screen); while upscaling is off it must
     * not be in the chain at all (the FPP's own final pass already covers
     * the full native screen then). No-op before initialize() has created
     * the filter.
     */
    private void syncCompositeFilter() {
        if (fsrCompositeFilter == null) {
            return;
        }
        boolean inChain = fpp.getFilterList().contains(fsrCompositeFilter);
        if (useUpscaling && !inChain) {
            // Appends at the end of the chain - after the runtime fog if
            // the weather already added it, until addFog() re-inserts the
            // fog before it - which is exactly the terminal position the
            // composite needs.
            fpp.addFilter(fsrCompositeFilter);
        } else if (!useUpscaling && inChain) {
            fpp.removeFilter(fsrCompositeFilter);
        }
    }

    private void setupFXAAFilter() {
        FXAAFilter fxaa = new FXAAFilter();
        fxaa.setSubPixelShift(5.0f);
        fxaa.setReduceMul(5.0f);
        fxaa.setVxOffset(5.0f);
        fxaa.setEnabled(true);
        fpp.addFilter(fxaa);
    }

    private void setupBloomFilter() {
        BloomFilter bloom = new BloomFilter(BloomFilter.GlowMode.SceneAndObjects);
        bloom.setDownSamplingFactor(2.0f);
        bloom.setExposurePower(55);
        bloom.setBloomIntensity(1.0f);
        fpp.addFilter(bloom);
    }

    private void setupFilterPostProcessor(AssetManager assetManager) {
        fpp = new FilterPostProcessor(assetManager);
    }

    /**
     * Reads the persisted upscaling settings (see
     * Constants.ADVANCED_VIDEO_*): the master toggle (default OFF, unlike the
     * other Advanced Video switches) and the quality preset (default
     * Quality).
     */
    private void readUpscalingSettings() {
        AppSettings settings = ((ANJRpgInterface) application).getSettings();
        useUpscaling = SettingsUtils.readToggle(settings, Constants.ADVANCED_VIDEO_UPSCALING, false);
        currentPreset = SettingsUtils.readUpscalingPreset(settings);
    }

    /**
     * The display resolution the game presents at. The CONTEXT settings are
     * live: the LWJGL3 context updates them on every window/framebuffer
     * resize, while the application-level AppSettings are not touched by
     * resizes.
     */
    private AppSettings liveSettings() {
        if (application.getContext() != null && application.getContext().getSettings() != null) {
            return application.getContext().getSettings();
        }
        // Headless / tests: fall back to the application settings.
        return ((ANJRpgInterface) application).getSettings();
    }

    /**
     * (Re-)derives the internal resolution from the current preset and
     * display resolution, applies the camera viewport rect and pushes the
     * dimensions into the FSR chain filters. Validates the preset against
     * the display resolution and falls back to the smallest valid preset
     * (Performance) or disables upscaling entirely when nothing is valid
     * (specification section 11.2, rule 3).
     *
     * @param rebuildFpp true to rebuild the FilterPostProcessor after the
     *                   camera rect change (runtime changes); false during
     *                   construction, where the FPP is not attached yet
     */
    private void applyUpscalingConfiguration(boolean rebuildFpp) {
        readDisplayResolution();
        deriveInternalResolution();
        applyViewportRect();
        snapInternalToFppDerivation();
        logUpscalingState();
        updateFsrFilterDimensions();

        // The composite filter is in the MAIN chain exactly while
        // upscaling is on (its material is what the present viewport
        // draws; on the native path the main FPP's own final pass already
        // covers the screen through the full camera rect). Sync it BEFORE
        // the rebuild so the processor surgery re-initializes the correct
        // chain. The present viewport is active exactly while upscaling
        // is on as well.
        syncCompositeFilter();
        syncPresentViewport();

        if (rebuildFpp) {
            rebuildFilterProcessor();
        }

        appliedUpscalingSignature = upscalingSignature();
    }

    private String upscalingSignature() {
        return useUpscaling + "|" + currentPreset + "|" + displayWidth + "x" + displayHeight;
    }

    /**
     * Computes the ideal internal resolution from the current preset,
     * with strict validation and fallback (spec 11.2, rule 3): an invalid
     * preset falls back to the smallest valid one (Performance), and if no
     * preset is valid at all, upscaling is disabled for the current display.
     */
    private void deriveInternalResolution() {
        internalWidth = displayWidth;
        internalHeight = displayHeight;

        if (useUpscaling) {
            ResolutionPreset preset = currentPreset;
            if (!preset.isValidFor(displayWidth, displayHeight)) {
                preset = ResolutionPreset.smallestValidFor(displayWidth, displayHeight);
                if (preset == null) {
                    LOGGER.warning("FSR upscaling disabled: no preset is valid for the current display resolution "
                            + displayWidth + "x" + displayHeight);
                    useUpscaling = false;
                } else {
                    LOGGER.warning("FSR upscaling preset " + currentPreset + " is not valid for display resolution "
                            + displayWidth + "x" + displayHeight + ", falling back to " + preset);
                    currentPreset = preset;
                }
            }
            if (useUpscaling) {
                internalWidth = currentPreset.computeWidth(displayWidth);
                internalHeight = currentPreset.computeHeight(displayHeight);
            }
        }
    }

    private void readDisplayResolution() {
        AppSettings settings = liveSettings();
        int width = settings != null ? settings.getWidth() : 0;
        int height = settings != null ? settings.getHeight() : 0;
        if (width <= 0 || height <= 0) {
            // Headless / not yet started: fall back to the camera, which the
            // engine keeps at the canvas resolution.
            Camera camera = viewPort.getCamera();
            width = camera.getWidth();
            height = camera.getHeight();
        }
        displayWidth = width;
        displayHeight = height;
    }

    /**
     * Sizes the main camera's viewport rect to the internal/display ratio.
     * The FilterPostProcessor derives all its buffer sizes from this rect:
     * a fraction of (internalWidth / displayWidth) makes the scene and all
     * pre-upscale filter passes render at the internal resolution. The
     * fraction is nudged by half a texel so the FPP's truncated
     * (int)(canvas * ratio) computation lands exactly on internalWidth even
     * with float rounding.
     */
    private void applyViewportRect() {
        Camera camera = viewPort.getCamera();
        if (!useUpscaling || internalWidth >= displayWidth || internalHeight >= displayHeight) {
            rectX = 1f;
            rectY = 1f;
            camera.setViewPort(0f, 1f, 0f, 1f);
            return;
        }
        rectX = (internalWidth + 0.5f) / (float) displayWidth;
        rectY = (internalHeight + 0.5f) / (float) displayHeight;
        camera.setViewPort(0f, rectX, 0f, rectY);
    }

    /**
     * The internal width the FPP will actually compute from the applied
     * viewport rect (identical truncation math to
     * FilterPostProcessor.reshape) - used so that the EASU InputSize always
     * matches the real low-res buffer size, even after display resizes that
     * the FPP handles from its saved rect without a rebuild.
     */
    private int fppDerivedWidth() {
        return Math.max(1, (int) (displayWidth * rectX));
    }

    /** See {@link #fppDerivedWidth()}. */
    private int fppDerivedHeight() {
        return Math.max(1, (int) (displayHeight * rectY));
    }

    /**
     * Aligns the stored internal resolution with the FPP's derivation from
     * the applied rect (a no-op in the normal path, since the rect is built
     * FROM the internal size; keeps the invariant explicit).
     */
    private void snapInternalToFppDerivation() {
        if (useUpscaling) {
            internalWidth = fppDerivedWidth();
            internalHeight = fppDerivedHeight();
        }
    }

    /**
     * Spec 11.2 rule 7: log every preset application - selected preset,
     * computed internal size, display size and validation result.
     */
    private void logUpscalingState() {
        LOGGER.log(useUpscaling ? Level.INFO : Level.FINE,
                "FSR upscaling: enabled={0} preset={1} internal={2}x{3} display={4}x{5} valid={6}",
                new Object[]{useUpscaling, currentPreset, internalWidth, internalHeight,
                        displayWidth, displayHeight, currentPreset.isValidFor(displayWidth, displayHeight)});
    }

    /**
     * Pushes the current internal/display dimensions into the FSR chain
     * filters. Must run BEFORE the FPP (re-)initializes its filters, so
     * their initFilter implementations see up-to-date values.
     */
    private void updateFsrFilterDimensions() {
        if (fsrEASUFilter != null) {
            fsrEASUFilter.setInputSize(internalWidth, internalHeight);
            fsrEASUFilter.setOutputSize(displayWidth, displayHeight);
            fsrEASUFilter.setEnabled(useUpscaling);
        }
        if (fsrCASFilter != null) {
            fsrCASFilter.setDisplayResolution(displayWidth, displayHeight);
        }
        if (filmGrainFilter != null) {
            filmGrainFilter.setDisplayResolution(displayWidth, displayHeight);
        }
        if (fog != null) {
            fog.setDisplayResolution(displayWidth, displayHeight);
        }
    }

    /**
     * Rebuilds the FilterPostProcessor at the current camera rect. The FPP
     * saves the viewport rect in private fields at initialization and
     * restores it on every reshape, so a runtime ratio change requires the
     * remove/re-add processor dance: removeProcessor() runs cleanup(), which
     * disposes every filter buffer and restores the camera to canvas size,
     * then the new rect is set and addProcessor() re-initializes the whole
     * chain at the new internal resolution on the next frame.
     */
    private void rebuildFilterProcessor() {
        viewPort.removeProcessor(fpp);
        applyViewportRect();
        viewPort.addProcessor(fpp);
    }

    /**
     * Display-resolution change notification (fullscreen toggle, window
     * resize, monitor change). Runs from the DisplayResolutionTracker BEFORE
     * the FPP's own reshape, so the FSR filters are re-initialized with
     * fresh dimensions. Recomputes the internal resolution for the current
     * preset (spec 11.2, rule 2) with validation and fallback (rule 3).
     */
    void onDisplayResolution(int width, int height) {
        if (width <= 0 || height <= 0 || (width == displayWidth && height == displayHeight)) {
            return;
        }
        displayWidth = width;
        displayHeight = height;

        if (!useUpscaling) {
            internalWidth = displayWidth;
            internalHeight = displayHeight;
            updateFsrFilterDimensions();
        } else if (!currentPreset.isValidFor(displayWidth, displayHeight)) {
            // Degenerate display size (ratio presets are valid for every
            // practical resolution): full re-validation, which may disable
            // upscaling, plus a processor rebuild to restore the full rect.
            applyUpscalingConfiguration(true);
            return;
        } else {
            // The FPP is about to rebuild its own buffers from its SAVED rect
            // fractions (the same values as rectX/rectY); derive the internal
            // size with the identical truncation so EASU's InputSize matches
            // the actual buffer size exactly. No processor surgery needed:
            // the FPP's reshape re-creates every buffer, and our filters read
            // the dimensions pushed below when their initFilter re-runs.
            internalWidth = fppDerivedWidth();
            internalHeight = fppDerivedHeight();
            logUpscalingState();
            updateFsrFilterDimensions();
        }

        // Keep the present viewport's camera at the display size. The
        // engine's notifyReshape only resizes cameras of viewports whose
        // output framebuffer is null - while the present FPP has its
        // filters enabled it redirects the viewport output, so the resize
        // must be done here (this runs from the DisplayResolutionTracker,
        // which is notified BEFORE both FPPs).
        if (presentViewPort != null) {
            Camera presentCamera = presentViewPort.getCamera();
            if (presentCamera.getWidth() != width || presentCamera.getHeight() != height) {
                presentCamera.resize(width, height, true);
            }
        }
        appliedUpscalingSignature = upscalingSignature();
    }

    /**
     * Runtime toggle for the FSR upscaling path (Advanced Video Settings ->
     * "FSR Upscaling"). When off, the scene renders natively at the display
     * resolution and the EASU filter is bypassed.
     */
    @Override
    public void setUpscalingEnabled(boolean enabled) {
        if (useUpscaling == enabled) {
            return;
        }
        useUpscaling = enabled;
        reconfigureUpscaling("runtime toggle");
    }

    /**
     * Selects the upscaling quality preset (relative to the CURRENT display
     * resolution; see {@link ResolutionPreset}). Applies immediately when
     * upscaling is enabled; when disabled the preset is only stored and has
     * no effect until upscaling is turned on (spec 11.2, rule 4).
     */
    @Override
    public void applyResolutionPreset(ResolutionPreset preset) {
        if (preset == null) {
            return;
        }
        currentPreset = preset;
        if (useUpscaling) {
            reconfigureUpscaling("preset change");
        }
    }

    @Override
    public boolean isUpscalingEnabled() {
        return useUpscaling;
    }

    @Override
    public ResolutionPreset getUpscalingPreset() {
        return currentPreset;
    }

    /** The effective internal render width (equals display width when upscaling is off). */
    int getInternalWidth() {
        return internalWidth;
    }

    /** The effective internal render height (equals display height when upscaling is off). */
    int getInternalHeight() {
        return internalHeight;
    }

    private void reconfigureUpscaling(String reason) {
        if (upscalingSignature().equals(appliedUpscalingSignature)) {
            // Nothing actually changed - still refresh the filter uniforms.
            updateFsrFilterDimensions();
            return;
        }
        LOGGER.fine(() -> "FSR upscaling reconfiguration (" + reason + ")");
        applyUpscalingConfiguration(true);
    }

    @Override
    public void initialize() {
        setupLightScatteringFilter();

        setupWaterFilter();

        // FSR 1 pipeline: the EASU upscaler goes AFTER the water filter (the
        // stock WaterFilter renders at the internal resolution) and the RCAS
        // sharpener IMMEDIATELY after it - nothing may run between EASU and
        // RCAS. Both run at the display resolution (see FSREASUFilter and
        // FSRCASFilter). FXAA already ran before, on the low-res image.
        setupFsrFilters();

        // Film grain goes after the water and the FSR chain: the add order
        // above is the render order, so the grain is now applied to the
        // FINAL image - scene, water, god rays and the upscaled, sharpened
        // picture - instead of being painted over by the water pass. It also
        // receives display-ready (already gamma-encoded) colors, which is
        // the space film_grain.frag v5 weights its grain in. Fog, added at
        // runtime by the weather, blends toward fog color only where the fog
        // is dense and passes the grained pixels through untouched - at the
        // display resolution when upscaling is on (DisplayResolutionFogFilter),
        // inserted before the terminal composite filter.
        setupFilmGrainFilter();

        // The terminal composite pass: while upscaling is on, its material
        // carries the final image that the present viewport draws over the
        // WHOLE screen (the FPP's stock final pass would only cover the
        // internal-size sub-rectangle). Added after the film grain - the
        // terminal position of the chain.
        setupFsrCompositeFilter();

        // The filters were created above - give them the current upscaling
        // dimensions (no-ops when upscaling is off).
        updateFsrFilterDimensions();
    }

    /**
     * The FSR 1 pair: FSREASUFilter (spatial upscaling, internal -> display
     * resolution) followed IMMEDIATELY by the existing FSRCASFilter (RCAS
     * sharpening on the upscaled image). Added here so they sit after the
     * Water filter in the chain.
     */
    private void setupFsrFilters() {
        fsrEASUFilter = new FSREASUFilter();
        fsrCASFilter = new FSRCASFilter();

        // Dimensions must be injected BEFORE addFilter: when the FPP is
        // already initialized (mid-load addFilter), the filter's initFilter
        // runs immediately, and the display size cannot be derived from the
        // viewport camera at that point - the FPP has already resized it to
        // the internal resolution for the frame.
        fsrEASUFilter.setInputSize(internalWidth, internalHeight);
        fsrEASUFilter.setOutputSize(displayWidth, displayHeight);
        fsrEASUFilter.setEnabled(useUpscaling);

        fsrCASFilter.setDisplayResolution(displayWidth, displayHeight);
        // RCAS sharpness: 0.0 = exact pass-through, 1.0 = the FSR 1
        // reference maximum (FSR_RCAS_LIMIT). The previous 0.0 turned the
        // whole RCAS stage off (part of the "no visible FSR CAS effect"
        // bug, together with the rewritten shader); 0.8 is a clearly
        // visible default sharpening strength.
        fsrCASFilter.setSharpness(0.8f);
        // The Advanced Video panel may have switched this off in the main menu,
        // before the world (and this manager) was loaded - honor it here.
        fsrCASFilter.setEnabled(advancedVideoEnabled(Constants.ADVANCED_VIDEO_FSR_CAS));

        fpp.addFilter(fsrEASUFilter);
        fpp.addFilter(fsrCASFilter);
    }

    private void setupFilmGrainFilter() {
        filmGrainFilter = new FilmGrainFilter(application);
        // Before addFilter, for the same reason as in setupFsrFilters().
        filmGrainFilter.setDisplayResolution(displayWidth, displayHeight);
        filmGrainFilter.setGrainRate(30f);
        // Honor a toggle made from the main menu before the world was loaded
        filmGrainFilter.setEnabled(advancedVideoEnabled(Constants.ADVANCED_VIDEO_FILM_GRAIN));
        fpp.addFilter(filmGrainFilter);
    }

    /**
     * The terminal filter of the FSR 1 chain: a plain copy whose material
     * the present viewport renders over the whole screen (see
     * {@link FSRCompositeFilter}). Created once at world load; in the main
     * chain exactly while upscaling is enabled (syncCompositeFilter keeps
     * that invariant across runtime toggles).
     */
    private void setupFsrCompositeFilter() {
        fsrCompositeFilter = new FSRCompositeFilter();
        if (useUpscaling) {
            fpp.addFilter(fsrCompositeFilter);
        }
    }

    /**
     * The FSR EASU upscaling filter. Added after Water and immediately
     * before FSRCASFilter; runtime toggling goes through
     * {@link Filter#setEnabled(boolean)} or
     * {@link #setUpscalingEnabled(boolean)}, never remove/add, which would
     * move it to the end of the chain and break the FSR order.
     */
    @Override
    public Filter getFsrEASUFilter() {
        return fsrEASUFilter;
    }

    /**
     * Reads an Advanced Video toggle from the settings; an absent key means
     * the default (on). See Constants.ADVANCED_VIDEO_*.
     */
    private boolean advancedVideoEnabled(String settingsKey) {
        AppSettings settings = ((ANJRpgInterface) application).getSettings();
        return SettingsUtils.readToggle(settings, settingsKey);
    }

    private void setupGammaCorrectionFilter() {
        gammaCorrectionFilter = new GammaCorrectionFilter();
        fpp.addFilter(gammaCorrectionFilter);
    }

    @Override
    public GammaCorrectionFilter getGammaCorrectionFilter() {
        return gammaCorrectionFilter;
    }

    /**
     * Film grain composites over the final image because it was added last
     * (after Water, see initialize()). Callers toggle it with setEnabled();
     * the chain position is preserved.
     */
    @Override
    public FilmGrainFilter getFilmGrainFilter() {
        return filmGrainFilter;
    }

    /**
     * FSR CAS runs immediately after the FSR EASU upscaler (FSR 1
     * requirement: RCAS sharpens the UPSCALED image). Callers toggle it with
     * setEnabled(); the chain position is preserved.
     */
    @Override
    public FSRCASFilter getFsrCASFilter() {
        return fsrCASFilter;
    }

    private void setupToneMapFilter() {
        ToneMapFilter toneMapFilter = new ToneMapFilter();
        toneMapFilter.setWhitePoint(new Vector3f(8f, 8f, 8f));
        fpp.addFilter(toneMapFilter);
    }

    private void setupFog() {
        // DisplayResolutionFogFilter: while FSR upscaling is active the fog
        // runs mid-chain (before the composite, after EASU/RCAS) and must
        // render at the display resolution instead of downsampling the
        // upscaled image to the internal one. See DisplayResolutionFogFilter.
        fog = new DisplayResolutionFogFilter();

        fog.setFogColor(new ColorRGBA(0.9f, 0.9f, 0.9f, 1.0f));
        fog.setFogDistance(5000);
        fog.setFogDensity(2.255f);
    }

    private void setupCartoonEdgeFilter() {
        CartoonEdgeFilter toon=new CartoonEdgeFilter();
        toon.setEdgeWidth(0.5f);
        toon.setEdgeIntensity(0.09f);
        toon.setNormalThreshold(0.8f);
        fpp.addFilter(toon);
    }

    private void setupWaterFilter() {
        // add an ocean.
        waterFilter = new WaterFilter(rootNode, sky.getSunDirection().normalize());
        waterFilter.setWaterHeight(Constants.WATER_LEVEL_HEIGHT);
        fpp.addFilter(waterFilter);

        // Pass reference to texture capturer
        if (tileTextureCapturer != null) {
            tileTextureCapturer.setMainWaterFilter(waterFilter);
        }
    }


    @Override
    public void addFog() {
        /** Add fog to a scene */
        // While FSR upscaling is active the fog must be inserted BEFORE the
        // composite filter, never appended after it: the FPP only appends
        // (there is no positional insert), and the composite must stay the
        // terminal filter so its material carries the FOGGED image the
        // present viewport draws. So: temporarily remove the composite,
        // append the fog, re-append the composite (re-adding re-initializes
        // it, which is cheap).
        if (fsrCompositeFilter != null && fpp.getFilterList().contains(fsrCompositeFilter)) {
            fpp.removeFilter(fsrCompositeFilter);
            fpp.addFilter(fog);
            fpp.addFilter(fsrCompositeFilter);
        } else {
            fpp.addFilter(fog);
        }
    }

    @Override
    public void removeFog() {
        fpp.removeFilter(fog);
    }

    private void setupShadowRenderer() {
        pssmRenderer = new PssmShadowRenderer(assetManager, 2048, 16);
        pssmRenderer.setShadowIntensity(0.55f);
        pssmRenderer.setFilterMode(PssmShadowRenderer.FilterMode.PCF8);
        pssmRenderer.setCompareMode(PssmShadowRenderer.CompareMode.Hardware);
        viewPort.addProcessor(pssmRenderer);
    }

    private void setupTranslucentBucketFilter() {
        fpp.addFilter(new TranslucentBucketFilter());
    }

    private void setupSSAOFilter() {
        SSAOFilter ssaoFilter = Constants.SSAO_FILTER_BASIC;
        fpp.addFilter(ssaoFilter);
    }

    private void setupDepthOfFieldFilter() {
        DepthOfFieldFilter dof=new DepthOfFieldFilter();
        dof.setFocusDistance(0);
        dof.setFocusRange(20);
        dof.setBlurScale(0.325f);
        fpp.addFilter(dof);
    }

    private void setupLightScatteringFilter() {
        lsf = new LightScatteringFilter(sky.getSunDirection().negate().normalizeLocal().mult(500));
        lsf.setLightDensity(1.0f);
        fpp.addFilter(lsf);
    }

    /** The FilterPostProcessor backing the main chain (exposed for headless chain-order tests). */
    FilterPostProcessor getFilterPostProcessor() {
        return fpp;
    }

    /** The present viewport (null in headless tests without a RenderManager). */
    ViewPort getPresentViewPort() {
        return presentViewPort;
    }

    /** The present viewport's full-screen quad. */
    Picture getPresentQuad() {
        return presentQuad;
    }

    /** The terminal composite filter (created at initialize(), even headless). */
    FSRCompositeFilter getFsrCompositeFilter() {
        return fsrCompositeFilter;
    }

    /** The runtime DisplayResolutionFogFilter (created in the constructor, added by the weather). */
    DisplayResolutionFogFilter getFog() {
        return fog;
    }

    @Override
    public void update(float tpf) {
        lsf.setLightPosition(sky.getSunDirection().normalize().normalize().mult(500));
        pssmRenderer.setDirection(sky.getSunDirection().negate().normalize().mult(500));
        waterFilter.setLightDirection(sky.getSunDirection().normalize().mult(500));
        //Backlight translucency: same live sun direction source as the other
        //sun-driven effects; GpuGrassMaterial computes the glow axis and the
        //day-night fade once per frame for all material clones.
        GpuGrassMaterial.setSharedSunDirection(sky.getSunDirection());
    }

    /**
     * A minimal SceneProcessor whose only job is to receive the engine's
     * reshape notifications (canvas/window resolution changes) and forward
     * them to {@link #onDisplayResolution(int, int)} BEFORE the FPP handles
     * the same reshape - processors are notified in the order they were
     * added, and this tracker is added before the FPP.
     */
    private final class DisplayResolutionTracker implements SceneProcessor {
        private boolean initialized;

        @Override
        public void initialize(RenderManager rm, ViewPort vp) {
            initialized = true;
            onDisplayResolution(vp.getCamera().getWidth(), vp.getCamera().getHeight());
        }

        @Override
        public void reshape(ViewPort vp, int w, int h) {
            onDisplayResolution(w, h);
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }

        @Override
        public void preFrame(float tpf) {
        }

        @Override
        public void postQueue(RenderQueue rq) {
        }

        @Override
        public void postFrame(com.jme3.texture.FrameBuffer out) {
        }

        @Override
        public void cleanup() {
            initialized = false;
        }

        @Override
        public void setProfiler(AppProfiler profiler) {
        }
    }
}
