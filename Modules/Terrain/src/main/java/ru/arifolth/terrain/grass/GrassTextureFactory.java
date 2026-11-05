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

package ru.arifolth.terrain.grass;

import com.jme3.asset.AssetManager;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.Texture3D;
import com.jme3.texture.image.ColorSpace;
import ru.arifolth.anjrpg.interfaces.Constants;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Procedurally generates the textures used by the GPU grass material:
 *
 *  - a grass color texture (dense green noise, tiles seamlessly);
 *  - a 3D mask texture with one alpha slice per grass layer. Each slice
 *    contains the same random point pattern (same seed) but fewer points
 *    and lower alpha than the previous one, so the grass gradually thins
 *    out towards the tips of the blades. Slices are sampled by the
 *    fragment shader with the layer index as the third texture coordinate.
 *
 * This is a cleaned-up port of the generateGrassTex/generateMasks code from
 * the original jme3-grass-shader demo, using the built-in
 * {@link Texture3D} that modern jME versions provide
 * (the old project had to ship its own Texture3D class).
 */
public class GrassTextureFactory {

    private static final Logger LOGGER = Logger.getLogger(GrassTextureFactory.class.getName());

    private static final int GRASS_TEX_SIZE = 200;
    private static final int MASK_SIZE = 200;
    private static final int MASK_AREA = MASK_SIZE * MASK_SIZE;

    // grass v1 blade shape: every mask point is drawn as a small DISC
    // (in mask texels) whose radius SHRINKS towards the tip slices, so a
    // blade column tapers into a thin straight triangle instead of a
    // stack of equal-width dashes. Base radius 1.6 texels x 4 cm texel
    // (GPU_GRASS_MASK_WORLD_SPAN = 8) = ~13 cm blade base width, which is
    // wider than the shell spacing (Length / layers = 10 cm) -> the shell
    // dots overlap vertically and read as ONE continuous blade from any
    // distance; only extreme zoom resolves the stacked shells.
    private static final float MASK_BLADE_BASE_RADIUS = 1.6f;
    private static final float MASK_BLADE_TAPER = 0.72f;

    private GrassTextureFactory() {
    }

    /**
     * Loads the grass shell color texture. An empty/null
     * {@link Constants#GPU_GRASS_TEXTURE_PATH} (the default) generates the
     * same green noise texture in code that the original example project
     * used; a configured path is loaded from the assets instead.
     */
    public static Texture loadGrassTexture(AssetManager assetManager) {
        String path = Constants.GPU_GRASS_TEXTURE_PATH;
        if (path != null && !path.isBlank()) {
            try {
                Texture grass = assetManager.loadTexture(path);
                grass.setWrap(Texture.WrapMode.Repeat);
                grass.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
                grass.setMagFilter(Texture.MagFilter.Bilinear);
                return grass;
            } catch (Exception e) {
                LOGGER.warning("Could not load " + path
                        + ", falling back to procedural grass texture: " + e.getMessage());
            }
        }

        Image grassTexImg = new Image(Image.Format.RGBA8, GRASS_TEX_SIZE, GRASS_TEX_SIZE,
                generateGrassData(), ColorSpace.sRGB);
        Texture2D grassTex = new Texture2D(grassTexImg);
        grassTex.setWrap(Texture.WrapMode.Repeat);
        grassTex.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        grassTex.setMagFilter(Texture.MagFilter.Bilinear);
        return grassTex;
    }

    private static ByteBuffer generateGrassData() {
        Random rand = new Random(20260101L);
        ByteBuffer data = ByteBuffer.allocateDirect(GRASS_TEX_SIZE * GRASS_TEX_SIZE * 4);
        for (int i = 0; i < GRASS_TEX_SIZE * GRASS_TEX_SIZE; i++) {
            int greenVariation = rand.nextInt(70);
            int blueVariation = rand.nextInt(20);
            data.put((byte) (18 + blueVariation));               //blue
            data.put((byte) (55 + greenVariation));              //green
            data.put((byte) (20 + rand.nextInt(20)));            //red
            data.put((byte) 255);                                //alpha
        }
        data.flip();
        return data;
    }

    /**
     * Builds the 3D layer mask: depth slices, slice i (0 = ground layer,
     * depth-1 = tip layer) holds a subset of the points of slice i-1 drawn
     * with a lower alpha value and a SMALLER DISC RADIUS, so blades taper
     * and thin towards the tip (grass v1: straight tapering blades).
     */
    public static Texture3D createThreeDLayerMask(int depth) {
        ByteBuffer texBuf3D = ByteBuffer.allocateDirect(MASK_AREA * depth);

        //same seed for every slice: every slice is a nested subset of the
        //previous one, so the mask columns line up into vertical blades
        long seed = 20260101L;
        Random rand = new Random(seed);

        for (int i = 0; i < depth; i++) {
            rand.setSeed(seed);

            float density = (depth <= 1) ? 0f : i / (float) (depth - 1);
            //bottom layer: the most points; top layer: the fewest
            int numGrass = (int) (5000 - (4200 * density));

            //alpha also decreases with height so the tips blend out.
            //NOTE: the material renders as alpha CUTOUT (fragments below
            //GPU_GRASS_ALPHA_CUTOFF are discarded), so the minimum tip alpha
            //must stay comfortably above that cutoff - hence 0.45, not 0.25.
            int alphaVal = (int) (255 * (1f - 0.45f * density));

            //grass v1: the disc radius shrinks towards the tip slices, so the
            //visible silhouette of a blade column narrows into a triangle
            //(base ~1.6 texels wide, tip ~0.45 texels). Same seed per slice
            //keeps the point positions nested across slices: the disc of one
            //blade always sits at the same (x, y) - only its size changes.
            float radius = MASK_BLADE_BASE_RADIUS * (1f - MASK_BLADE_TAPER * density);
            int r = (int) Math.ceil(radius);
            float rSq = radius * radius;

            for (int j = 0; j < numGrass; j++) {
                int curPoint = rand.nextInt(MASK_AREA);
                int px = curPoint % MASK_SIZE;
                int py = curPoint / MASK_SIZE;
                //filled disc, wrapping at the tile edges (the mask repeats
                //S/T, so wrapped dots keep the pattern seamless)
                for (int dy = -r; dy <= r; dy++) {
                    int yy = (py + dy + MASK_SIZE) % MASK_SIZE;
                    for (int dx = -r; dx <= r; dx++) {
                        if (dx * dx + dy * dy > rSq) {
                            continue;
                        }
                        int xx = (px + dx + MASK_SIZE) % MASK_SIZE;
                        texBuf3D.put(i * MASK_AREA + yy * MASK_SIZE + xx, (byte) alphaVal);
                    }
                }
            }
        }

        texBuf3D.flip();

        ArrayList<ByteBuffer> bb3d = new ArrayList<>();
        bb3d.add(texBuf3D);
        Image tex3DImg = new Image(Image.Format.Alpha8, MASK_SIZE, MASK_SIZE, depth, bb3d,
                ColorSpace.Linear);
        Texture3D texture3D = new Texture3D(tex3DImg);
        //S/T repeat so the pattern tiles across the terrain;
        //R clamped so the topmost layer (z = 1.0) samples the last slice
        //instead of wrapping back to the densest ground slice
        texture3D.setWrap(Texture.WrapAxis.S, Texture.WrapMode.Repeat);
        texture3D.setWrap(Texture.WrapAxis.T, Texture.WrapMode.Repeat);
        texture3D.setWrap(Texture.WrapAxis.R, Texture.WrapMode.EdgeClamp);
        texture3D.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        texture3D.setMagFilter(Texture.MagFilter.Bilinear);
        return texture3D;
    }

    /**
     * Builds plain 2D masks for the (optional) non-3D-texture rendering path.
     * One mask per numMasks entry, each holding fewer points than the previous.
     */
    public static List<Texture> createLayerMasks(int numMasks) {
        List<Texture> masks = new ArrayList<>();
        long seed = 20260101L;
        Random rand = new Random(seed);

        for (int i = 0; i < numMasks; i++) {
            rand.setSeed(seed);

            ByteBuffer data = ByteBuffer.allocateDirect(MASK_AREA);

            float density = (numMasks <= 1) ? 0f : i / (float) (numMasks - 1);
            int numGrass = (int) (5000 - (4200 * density));
            int alphaVal = (int) (255 * (1f - 0.75f * density));

            //grass v1: same tapering discs as the 3D mask (see above)
            float radius = MASK_BLADE_BASE_RADIUS * (1f - MASK_BLADE_TAPER * density);
            int r = (int) Math.ceil(radius);
            float rSq = radius * radius;

            for (int j = 0; j < numGrass; j++) {
                int curPoint = rand.nextInt(MASK_AREA);
                int px = curPoint % MASK_SIZE;
                int py = curPoint / MASK_SIZE;
                for (int dy = -r; dy <= r; dy++) {
                    int yy = (py + dy + MASK_SIZE) % MASK_SIZE;
                    for (int dx = -r; dx <= r; dx++) {
                        if (dx * dx + dy * dy > rSq) {
                            continue;
                        }
                        int xx = (px + dx + MASK_SIZE) % MASK_SIZE;
                        data.put(yy * MASK_SIZE + xx, (byte) alphaVal);
                    }
                }
            }

            Image grassMask = new Image(Image.Format.Alpha8, MASK_SIZE, MASK_SIZE, data,
                    ColorSpace.Linear);
            Texture2D texToAdd = new Texture2D(grassMask);
            texToAdd.setWrap(Texture.WrapMode.Repeat);
            texToAdd.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
            texToAdd.setMagFilter(Texture.MagFilter.Bilinear);
            masks.add(texToAdd);
        }
        return masks;
    }
}
