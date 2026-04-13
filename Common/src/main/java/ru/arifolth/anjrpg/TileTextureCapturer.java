/**
 * ANJRpg - an open source Role Playing Game written in Java.
 * Copyright (C) 2014 - 2025 Alexander Nilov
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package ru.arifolth.anjrpg;

import com.jme3.app.Application;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;
import com.jme3.bounding.BoundingBox;
import com.jme3.light.DirectionalLight;
import com.jme3.light.AmbientLight;
import com.jme3.water.WaterFilter;
import ru.arifolth.anjrpg.interfaces.ANJRpgInterface;
import ru.arifolth.anjrpg.interfaces.GameLogicCoreInterface;
import ru.arifolth.anjrpg.interfaces.camera.FollowCameraInterface;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Captures terrain tiles as textures for world map display.
 *
 * FIXED VERSION: Correct camera frustum bounds for each subtile quadrant
 */
public class TileTextureCapturer {
    private static final Logger LOGGER = Logger.getLogger(TileTextureCapturer.class.getName());
    private static final int SUBTILE_RESOLUTION = 512;
    private static final int CAPTURE_DELAY_FRAMES = 1;

    private final Application app;
    private final GameLogicCoreInterface gameLogicCore;
    private final FollowCameraInterface camera;

    private WaterFilter mainWaterFilter; // Reference to the main water filter

    // Add a new queue for sub‑tile capture tasks
    private final Queue<SubTileCaptureTask> subTileCaptureQueue = new ConcurrentLinkedQueue<>();

    // Task definition
    private static class SubTileCaptureTask {
        final Spatial spatial;
        final int subX;
        final int subZ;
        final String subTileId;

        SubTileCaptureTask(Spatial spatial, int subX, int subZ, String subTileId) {
            this.spatial = spatial;
            this.subX = subX;
            this.subZ = subZ;
            this.subTileId = subTileId;
        }
    }

    /**
     * Lag‑free synchronous capture: enqueues four sub‑tile tasks to be processed
     * one per frame on the render thread.
     */
    synchronized public void syncCaptureTileTextures(Spatial spatial, String baseTileId) {
        if (spatial == null || baseTileId == null || baseTileId.isEmpty()) {
            LOGGER.warning("Invalid spatial or baseTileId for texture capture");
            return;
        }

        String[] parts = baseTileId.replace("tile_", "").split("_");
        if (parts.length != 2) {
            LOGGER.warning("Invalid base tile ID format: " + baseTileId);
            return;
        }

        try {
            int baseX = Integer.parseInt(parts[0]);
            int baseZ = Integer.parseInt(parts[1]);

            for (int subX = 0; subX < 2; subX++) {
                for (int subZ = 0; subZ < 2; subZ++) {
                    String subTileId = String.format("tile_%d_%d_sub_%d_%d", baseX, baseZ, subX, subZ);
                    subTileCaptureQueue.offer(new SubTileCaptureTask(spatial, subX, subZ, subTileId));
                }
            }
            LOGGER.info("Enqueued 4 sub‑tile captures for base tile: " + baseTileId);

        } catch (NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Failed to parse tile coordinates: " + baseTileId, e);
        }
    }

    /**
     * Process one sub‑tile capture per frame (called from simpleUpdate).
     * This spreads the workload and keeps the UI responsive.
     */
    public void processPendingSubTileCaptures() {
        SubTileCaptureTask task = subTileCaptureQueue.poll();
        if (task != null) {
            captureSingleSubTile(task.spatial, task.subX, task.subZ, task.subTileId);
        }
    }

    public TileTextureCapturer(Application app) {
        this.app = app;
        this.gameLogicCore = ((ANJRpgInterface) app).getGameLogicCore();
        camera = gameLogicCore.getFreeFollowCamera();

        mkWorldMapDir();
    }

    private void mkWorldMapDir() {
        try {
            Files.createDirectories(Paths.get("./WorldMap"));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to create WorldMap directory", e);
        }
    }

    /**
     * FIXED VERSION: Proper camera frustum bounds for each subtile quadrant
     *
     * Key changes:
     * 1. Correct calculation of subTileExtent (should be half of base tile extent)
     * 2. Proper camera frustum bounds based on which quadrant we're capturing
     * 3. Camera positioned at subtile center, not base tile center
     */
    synchronized private void captureSingleSubTile(Spatial spatial, int subX, int subZ, String subTileId) {
        FrameBuffer fb = null;
        ViewPort offscreenView = null;
        Node isolatedScene = null;
        Spatial clonedSpatial = null;
        FilterPostProcessor fpp = null;

        RenderManager renderManager = app.getRenderManager();
        camera.setEnabled(false);
        try {
            BoundingBox parentBounds = (BoundingBox) spatial.getWorldBound();
            if (parentBounds == null) {
                LOGGER.warning("Spatial has no valid bounds: " + subTileId);
                return;
            }

            Vector3f parentCenter = parentBounds.getCenter();
            Vector3f parentExtent = new Vector3f(parentBounds.getExtent(new Vector3f()));

            // Calculate sub-tile extents
            float subTileExtentX = parentExtent.x / 2.0f;
            float subTileExtentZ = parentExtent.z / 2.0f;

            // Calculate sub-tile center (account for coordinate system)
            float subTileCenterX = parentCenter.x + (subX == 0 ? -subTileExtentX : subTileExtentX);
            float subTileCenterZ = parentCenter.z + (subZ == 0 ? -subTileExtentZ : subTileExtentZ);

            Vector3f subTileCenter = new Vector3f(subTileCenterX, parentCenter.y, subTileCenterZ);
            Vector3f subTileExtent = new Vector3f(subTileExtentX, parentExtent.y, subTileExtentZ);

            // Clone for isolation (does NOT affect original)
            clonedSpatial = spatial.clone(false);
            if (clonedSpatial instanceof Node) {
                Node node = (Node) clonedSpatial;
                boolean hasGeometry = false;
                for (Spatial child : node.getChildren()) {
                    if (child instanceof com.jme3.scene.Geometry) {
                        hasGeometry = true;
                        break;
                    }
                }
                if (!hasGeometry) {
                    LOGGER.warning("No geometry found in subtile " + subTileId + ", skipping capture");
                    return;
                }
            }
            clonedSpatial.removeFromParent();
            clonedSpatial.updateGeometricState();

            // Create isolated scene
            isolatedScene = new Node("IsolatedSubTileScene_" + subTileId);
            isolatedScene.attachChild(clonedSpatial);

            // Add lighting
            DirectionalLight sun = new DirectionalLight();
            sun.setDirection(new Vector3f(-0.5f, -1.0f, -0.5f).normalizeLocal());
            sun.setColor(ColorRGBA.White.mult(1.0f));
            isolatedScene.addLight(sun);

            AmbientLight ambient = new AmbientLight();
            ambient.setColor(ColorRGBA.White.mult(0.6f));
            isolatedScene.addLight(ambient);

            WaterFilter offscreenWaterFilter = new WaterFilter();
            offscreenWaterFilter.setReflectionScene(isolatedScene);
            offscreenWaterFilter.setWaterHeight(mainWaterFilter.getWaterHeight());
            offscreenWaterFilter.setCausticsTexture(mainWaterFilter.getCausticsTexture());

            isolatedScene.updateLogicalState(0.016f);
            isolatedScene.updateGeometricState();

            Camera subTileCam = createCameraForSubTile(subTileCenter, subTileExtent, subX, subZ);

            // Create framebuffer
            Texture2D offscreenTexture = new Texture2D(SUBTILE_RESOLUTION, SUBTILE_RESOLUTION, Image.Format.RGBA8);
            offscreenTexture.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
            offscreenTexture.setMagFilter(Texture.MagFilter.Bilinear);

            fb = new FrameBuffer(SUBTILE_RESOLUTION, SUBTILE_RESOLUTION, 1);
            fb.setDepthBuffer(Image.Format.Depth);
            fb.addColorTexture(offscreenTexture);

            offscreenView = renderManager.createMainView("CaptureViewPort_" + subTileId, subTileCam);
            offscreenView.setClearFlags(true, true, true);
            offscreenView.setBackgroundColor(new ColorRGBA(0.15f, 0.15f, 0.15f, 1.0f));
            offscreenView.attachScene(isolatedScene);
            offscreenView.setOutputFrameBuffer(fb);

            fpp = new FilterPostProcessor(app.getAssetManager());
            fpp.addFilter(offscreenWaterFilter);
            offscreenView.addProcessor(fpp);

            // Force the post-processor to initialise
            fpp.initialize(renderManager, offscreenView);

            try {
                // Warm-up renders – allows WaterFilter internal processors to stabilise
                for (int i = 0; i < 4; i++) {
                    renderManager.renderViewPort(offscreenView, 0.016f);
                }
                renderManager.renderViewPort(offscreenView, 0.016f);
            } finally {
                // Remove the viewport
                renderManager.removeMainView(offscreenView);
            }

            // Read framebuffer
            ByteBuffer byteBuffer = BufferUtils.createByteBuffer(SUBTILE_RESOLUTION * SUBTILE_RESOLUTION * 4);
            renderManager.getRenderer().readFrameBuffer(fb, byteBuffer);

            Image image = new Image(Image.Format.RGBA8, SUBTILE_RESOLUTION, SUBTILE_RESOLUTION, byteBuffer);
            Texture2D perfectTexture = saveImageToFile(image, subTileId, subX, subZ);

            if (perfectTexture != null) {
                gameLogicCore.getTextureCache().storeTexture(subTileId, perfectTexture);
            } else {
                // Fallback if writing failed
                LOGGER.log(Level.SEVERE, "Fallback to offscreenTexture");
                gameLogicCore.getTextureCache().storeTexture(subTileId, offscreenTexture);
            }

//            LOGGER.info("Successfully captured sub-tile: " + subTileId);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to capture sub-tile " + subTileId, e);

        } finally {
            camera.setEnabled(true);

            if (fpp != null) {
                fpp.cleanup();
            }
            if (fb != null) {
                fb.dispose();
            }
            if (isolatedScene != null) {
                isolatedScene.detachAllChildren();
            }
            // ViewPort is already removed; but ensure it's detached
            if (offscreenView != null) {
                renderManager.removeMainView(offscreenView);
            }
        }
    }


    /**
     * FIXED: Camera frustum bounds for orthographic projection
     *
     * For an orthographic camera looking straight down:
     * - left/right bounds define X-axis coverage
     * - top/bottom bounds define Z-axis coverage (note: top=positive Z, bottom=negative Z)
     * - We offset these bounds based on which quadrant we're capturing
     */
    private Camera createCameraForSubTile(Vector3f subTileCenter, Vector3f subTileExtent, int subX, int subZ) {
        Camera cam = new Camera(SUBTILE_RESOLUTION, SUBTILE_RESOLUTION);

        // Position camera above the sub-tile
        float height = subTileCenter.y + Math.max(Math.max(subTileExtent.x, subTileExtent.z), subTileExtent.y) * 2.0f;
        cam.setLocation(new Vector3f(subTileCenter.x, height, subTileCenter.z));
        cam.lookAt(subTileCenter, Vector3f.UNIT_Y);

        // Orthographic projection
        cam.setParallelProjection(true);

        // Set frustum bounds
        float halfWidth = subTileExtent.x;
        float halfDepth = subTileExtent.z;

        float left = -halfWidth;
        float right = halfWidth;
        float top = halfDepth;
        float bottom = -halfDepth;
        float near = 0.1f;
        float far = height * 2.0f + 1000.0f;

        cam.setFrustum(near, far, left, right, top, bottom);

        return cam;
    }

    private java.awt.image.BufferedImage cropFixedQuadrant(Image jmeImage, int subX, int subZ) {
        // Convert JME Image to BufferedImage (implicit vertical flip already present)
        ByteBuffer buffer = jmeImage.getData(0);
        int width = jmeImage.getWidth();
        int height = jmeImage.getHeight();
        java.awt.image.BufferedImage bufferedImage = new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        buffer.rewind();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = buffer.get() & 0xFF;
                int g = buffer.get() & 0xFF;
                int b = buffer.get() & 0xFF;
                int a = buffer.get() & 0xFF;
                bufferedImage.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }

        int half = width / 2; // 256

        int cropX, cropY;
        if (subX == 0 && subZ == 0) {
            cropX = 0;    cropY = half; // bottom-left
        } else if (subX == 1 && subZ == 0) {
            cropX = half; cropY = half; // bottom-right
        } else if (subX == 0 && subZ == 1) {
            cropX = 0;    cropY = 0;    // top-left
        } else { // subX == 1 && subZ == 1
            cropX = half; cropY = 0;    // top-right
        }

        java.awt.image.BufferedImage cropped = bufferedImage.getSubimage(cropX, cropY, half, half);

        // Upscale to 512×512
        java.awt.image.BufferedImage result = new java.awt.image.BufferedImage(SUBTILE_RESOLUTION, SUBTILE_RESOLUTION, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2d = result.createGraphics();
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.drawImage(cropped, 0, 0, SUBTILE_RESOLUTION, SUBTILE_RESOLUTION, null);
        g2d.dispose();
        return result;
    }

    private Texture2D saveImageToFile(Image image, String subTileId, int subX, int subZ) throws IOException {
        java.awt.image.BufferedImage croppedImage = cropFixedQuadrant(image, subX, subZ);

        java.awt.image.BufferedImage flippedImage = flipHorizontal(croppedImage);

        /*File outputFile = new File("./WorldMap/" + subTileId + ".png");
        boolean success = javax.imageio.ImageIO.write(flippedImage, "PNG", outputFile);
        if (!success) {
            LOGGER.warning("ImageIO.write returned false for " + subTileId);
        }*/

        // Convert back to JME Texture2D
        int w = flippedImage.getWidth();
        int h = flippedImage.getHeight();
        ByteBuffer newBuffer = com.jme3.util.BufferUtils.createByteBuffer(w * h * 4);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = flippedImage.getRGB(x, y);
                newBuffer.put((byte) ((argb >> 16) & 0xFF)); // R
                newBuffer.put((byte) ((argb >> 8) & 0xFF));  // G
                newBuffer.put((byte) (argb & 0xFF));         // B
                newBuffer.put((byte) ((argb >> 24) & 0xFF)); // A
            }
        }
        newBuffer.flip();

        Image finalJmeImage = new Image(Image.Format.RGBA8, w, h, newBuffer);
        return new Texture2D(finalJmeImage);
    }

    /**
     * Flips a BufferedImage horizontally.
     */
    private java.awt.image.BufferedImage flipHorizontal(java.awt.image.BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        java.awt.image.BufferedImage flipped = new java.awt.image.BufferedImage(w, h, src.getType());
        java.awt.Graphics2D g2d = flipped.createGraphics();
        g2d.drawImage(src, 0, 0, w, h, w, 0, 0, h, null); // Draw mirrored
        g2d.dispose();
        return flipped;
    }

    public void setMainWaterFilter(WaterFilter waterFilter) {
        this.mainWaterFilter = waterFilter;
    }
}
