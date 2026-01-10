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
import com.jme3.renderer.Camera;
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
import ru.arifolth.anjrpg.interfaces.ANJRpgInterface;
import ru.arifolth.anjrpg.interfaces.GameLogicCoreInterface;

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
    private static final Logger logger = Logger.getLogger(TileTextureCapturer.class.getName());
    private static final int SUBTILE_RESOLUTION = 512;
    private static final int CAPTURE_DELAY_FRAMES = 10;

    private final Application app;
    private final GameLogicCoreInterface gameLogicCore;

    private static class PendingCapture {
        Spatial spatial;
        String baseTileId;
        int framesRemaining;

        PendingCapture(Spatial spatial, String baseTileId) {
            this.spatial = spatial;
            this.baseTileId = baseTileId;
            this.framesRemaining = CAPTURE_DELAY_FRAMES;
        }
    }

    private final Queue<PendingCapture> pendingCaptures =
            new ConcurrentLinkedQueue<>();

    public TileTextureCapturer(Application app) {
        this.app = app;
        this.gameLogicCore = ((ANJRpgInterface) app).getGameLogicCore();
        ensureWorldMapDirectory();
    }

    private void ensureWorldMapDirectory() {
        try {
            Files.createDirectories(Paths.get("./WorldMap"));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to create WorldMap directory", e);
        }
    }

    public void captureTileTextures(Spatial spatial, String baseTileId) {
        if (spatial == null || baseTileId == null || baseTileId.isEmpty()) {
            logger.warning("Invalid spatial or baseTileId for texture capture");
            return;
        }

        pendingCaptures.offer(new PendingCapture(spatial, baseTileId));
        logger.info("Queued tile for deferred capture: " + baseTileId);
    }

    public void processPendingCaptures() {
        if (pendingCaptures.isEmpty()) {
            return;
        }

        PendingCapture pending = pendingCaptures.peek();
        if (pending == null) {
            return;
        }

        pending.framesRemaining--;

        if (pending.framesRemaining <= 0) {
            pendingCaptures.poll();

            String[] parts = pending.baseTileId.replace("tile_", "").split("_");
            if (parts.length != 2) {
                logger.warning("Invalid base tile ID format: " + pending.baseTileId);
                return;
            }

            try {
                int baseX = Integer.parseInt(parts[0]);
                int baseZ = Integer.parseInt(parts[1]);

                for (int subX = 0; subX < 2; subX++) {
                    for (int subZ = 0; subZ < 2; subZ++) {
                        String subTileId = String.format("tile_%d_%d_sub_%d_%d", baseX, baseZ, subX, subZ);
                        captureSingleSubTile(pending.spatial, baseX, baseZ, subX, subZ, subTileId);
                    }
                }

//                logger.info("✓ Captured 4 sub-tiles for base tile: " + pending.baseTileId);

            } catch (NumberFormatException e) {
                logger.log(Level.SEVERE, "Failed to parse tile coordinates: " + pending.baseTileId, e);
            }
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
    private void captureSingleSubTile(Spatial spatial, int baseX, int baseZ,
                                      int subX, int subZ, String subTileId) {
        FrameBuffer fb = null;
        ViewPort offscreenView = null;
        Node isolatedScene = null;
        Spatial clonedSpatial = null;

        try {
            // Update original spatial to get valid bounds
            spatial.updateGeometricState();

            BoundingBox parentBounds = (BoundingBox) spatial.getWorldBound();
            if (parentBounds == null) {
                logger.warning("Spatial has no valid bounds: " + subTileId);
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
            clonedSpatial = spatial.clone();
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

            isolatedScene.updateLogicalState(0.016f);
            isolatedScene.updateGeometricState();

            // Create camera (ISOLATED - does not affect main game camera)
            Camera subTileCam = createCameraForSubTile(subTileCenter, subTileExtent, subX, subZ);

            // Create framebuffer
            Texture2D offscreenTexture = new Texture2D(SUBTILE_RESOLUTION, SUBTILE_RESOLUTION, Image.Format.RGBA8);
            offscreenTexture.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
            offscreenTexture.setMagFilter(Texture.MagFilter.Bilinear);

            fb = new FrameBuffer(SUBTILE_RESOLUTION, SUBTILE_RESOLUTION, 1);
            fb.setDepthBuffer(Image.Format.Depth);
            fb.addColorTexture(offscreenTexture);

            // Create ISOLATED viewport (crucial: use a unique name, attach to cloned scene ONLY)
            offscreenView = app.getRenderManager().createMainView("CaptureViewPort_" + subTileId, subTileCam);
            offscreenView.setClearFlags(true, true, true);
            offscreenView.setBackgroundColor(new ColorRGBA(0.15f, 0.15f, 0.15f, 1.0f));
            offscreenView.attachScene(isolatedScene);
            offscreenView.setOutputFrameBuffer(fb);

            // CRITICAL: Render to offscreen buffer ONLY, do NOT call app.getRenderManager().render()
            // This ensures the viewport doesn't interfere with main game rendering
            app.getRenderManager().renderViewPort(offscreenView, 0.016f);

            // Read framebuffer
            ByteBuffer byteBuffer = BufferUtils.createByteBuffer(SUBTILE_RESOLUTION * SUBTILE_RESOLUTION * 4);
            app.getRenderManager().getRenderer().readFrameBuffer(fb, byteBuffer);

            Image image = new Image(Image.Format.RGBA8, SUBTILE_RESOLUTION, SUBTILE_RESOLUTION, byteBuffer);
            saveImageToFile(image, subTileId);

            offscreenTexture.setImage(image);
            gameLogicCore.getTextureCache().storeTexture(subTileId, offscreenTexture);

//            logger.info("Successfully captured sub-tile: " + subTileId);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to capture sub-tile " + subTileId, e);

        } finally {
            // CLEANUP: Remove viewport IMMEDIATELY to prevent jitter
            if (offscreenView != null) {
                app.getRenderManager().removeMainView(offscreenView);
            }
            if (fb != null) {
                fb.dispose();
            }
            if (isolatedScene != null) {
                isolatedScene.detachAllChildren();
            }
            // clonedSpatial will be garbage collected with isolatedScene
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

    /**
     * SIMPLE: Detect terrain, scale to fill 512×512. That's it.
     */
    private java.awt.image.BufferedImage cropTerrainContent(
            Image jmeImage,
            int bgColorRGB,
            int tolerance) {

        try {
            ByteBuffer buffer = jmeImage.getData(0);
            int width = jmeImage.getWidth();
            int height = jmeImage.getHeight();

            java.awt.image.BufferedImage bufferedImage =
                    new java.awt.image.BufferedImage(width, height,
                            java.awt.image.BufferedImage.TYPE_INT_ARGB);

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

            int bgR = (bgColorRGB >> 16) & 0xFF;
            int bgG = (bgColorRGB >> 8) & 0xFF;
            int bgB = bgColorRGB & 0xFF;

            // Find bounds of non-background pixels
            int minX = width, maxX = -1;
            int minY = height, maxY = -1;

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb = bufferedImage.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;

                    if (colorDistanceSq(r, g, b, bgR, bgG, bgB) > tolerance * tolerance) {
                        minX = Math.min(minX, x);
                        maxX = Math.max(maxX, x);
                        minY = Math.min(minY, y);
                        maxY = Math.max(maxY, y);
                    }
                }
            }

            // Validate
            if (minX > maxX || minY > maxY) {
                logger.warning("No terrain content detected");
                return bufferedImage;
            }

            int croppedWidth = maxX - minX + 1;
            int croppedHeight = maxY - minY + 1;

//            logger.info(String.format("Detected terrain: %dx%d at (%d,%d)", croppedWidth, croppedHeight, minX, minY));

            // Extract the detected region
            java.awt.image.BufferedImage croppedImage =
                    bufferedImage.getSubimage(minX, minY, croppedWidth, croppedHeight);

            // Create result image
            java.awt.image.BufferedImage result =
                    new java.awt.image.BufferedImage(width, height,
                            java.awt.image.BufferedImage.TYPE_INT_ARGB);

            // Fill background
            int bgARGB = 0xFF000000 | bgColorRGB;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    result.setRGB(x, y, bgARGB);
                }
            }

            // JUST SCALE TO FILL: No centering, no margins, no complexity
            java.awt.Graphics2D g2d = result.createGraphics();
            g2d.setRenderingHint(
                    java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);

            // Scale to 85% to preserve some border for seamless tiling
//            int targetSize = (int) (512 * 0.98);
//            int offset = (512 - targetSize) / 2;
            int targetSize = 512;
            int offset = 0;

            g2d.drawImage(croppedImage, offset, offset, targetSize, targetSize, null);
            g2d.dispose();

//            logger.info("Scaled to fill 512×512");
            return result;

        } catch (Exception e) {
            logger.log(Level.WARNING, "Cropping failed", e);
            // Fallback
            ByteBuffer buffer = jmeImage.getData(0);
            int width = jmeImage.getWidth();
            int height = jmeImage.getHeight();
            java.awt.image.BufferedImage result =
                    new java.awt.image.BufferedImage(width, height,
                            java.awt.image.BufferedImage.TYPE_INT_ARGB);
            buffer.rewind();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int r = buffer.get() & 0xFF;
                    int g = buffer.get() & 0xFF;
                    int b = buffer.get() & 0xFF;
                    int a = buffer.get() & 0xFF;
                    result.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
                }
            }
            return result;
        }
    }

    private int colorDistanceSq(int r1, int g1, int b1, int r2, int g2, int b2) {
        int dr = r1 - r2;
        int dg = g1 - g2;
        int db = b1 - b2;
        return dr * dr + dg * dg + db * db;
    }

    private void saveImageToFile(Image image, String subTileId) {
        try {
            // Apply intelligent cropping
            // Background color from your offscreenView.setBackgroundColor: 0.15, 0.15, 0.15
            int bgColorRGB = 0x262626; // 0.15 * 255 ≈ 38 = 0x26
            int tolerance = 25; // Allow slight variation

            java.awt.image.BufferedImage croppedImage =
                    cropTerrainContent(image, bgColorRGB, tolerance);

            File outputFile = new File("./WorldMap/" + subTileId + ".png");
            boolean success = javax.imageio.ImageIO.write(croppedImage, "PNG", outputFile);

            if (success) {
//                logger.info(String.format("Saved cropped sub-tile: %s (%d bytes)", outputFile.getAbsolutePath(), outputFile.length()));
            } else {
                logger.warning("ImageIO.write returned false for " + subTileId);
            }

        } catch (java.io.IOException e) {
            logger.log(Level.SEVERE, "Failed to save image for sub-tile " + subTileId, e);
        }
    }

}