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
package se.jod.biomonkey.image;

import com.jme3.math.FastMath;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.plugins.AWTLoader;
import com.jme3.util.BufferUtils;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import jme3tools.converters.ImageToAwt;

/**
 * Scale mipmap alpha. Based on jme3tools.converters.MipMapGenerator in the
 * jME3-desktop package.
 *
 * This class does variable scaling of the mipmap alpha channels. It is a
 * temporary, non-optimized version that basically just goes through each mip
 * buffer and scales the alpha values. It uses the AWT image conversion code
 * like the regular mipmap generator.
 *
 * @author Andreas
 */
public class AlphaMipmapGenerator {

    private static BufferedImage scaleDown(BufferedImage sourceImage, int targetWidth, int targetHeight) {
        int sourceWidth = sourceImage.getWidth();
        int sourceHeight = sourceImage.getHeight();

        BufferedImage targetImage = new BufferedImage(targetWidth, targetHeight, sourceImage.getType());

        Graphics2D g = targetImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(sourceImage, 0, 0, targetWidth, targetHeight, 0, 0, sourceWidth, sourceHeight, null);
        g.dispose();

        return targetImage;
    }

    public static void resizeToPowerOf2(Image image) {
        BufferedImage original = ImageToAwt.convert(image, false, true, 0);
        int potWidth = FastMath.nearestPowerOfTwo(image.getWidth());
        int potHeight = FastMath.nearestPowerOfTwo(image.getHeight());
        int potSize = Math.max(potWidth, potHeight);

        BufferedImage scaled = scaleDown(original, potSize, potSize);

        AWTLoader loader = new AWTLoader();
        Image output = loader.load(scaled, false);

        image.setWidth(potSize);
        image.setHeight(potSize);
        image.setDepth(0);
        image.setData(output.getData(0));
        image.setFormat(output.getFormat());
        image.setMipMapSizes(null);
    }
    
    public static void generateMipMaps(Image image){
        generateMipMaps(image,1.5f);
    }

    public static void generateMipMaps(Image image, float alphaMult) {
        int alphaChannel = 0;
        if (image.getFormat().equals(Format.ABGR8)) {
            alphaChannel = 3;
        } else if (image.getFormat().equals(Format.RGBA8)) {
            alphaChannel = 0;
        } else {
            throw new RuntimeException("Image format not supported");
        }
        BufferedImage original = ImageToAwt.convert(image, false, true, 0);
        int width = original.getWidth();
        int height = original.getHeight();
        int level = 0;

        BufferedImage current = original;
        AWTLoader loader = new AWTLoader();
        ArrayList<ByteBuffer> output = new ArrayList<ByteBuffer>();
        int totalSize = 0;
        Format format = null;

        while (height >= 1 || width >= 1) {
            Image converted = loader.load(current, false);
            format = converted.getFormat();
            output.add(converted.getData(0));
            totalSize += converted.getData(0).capacity();

            if (height == 1 || width == 1) {
                break;
            }

            level++;

            height /= 2;
            width /= 2;

            current = scaleDown(current, width, height);
        }

        ByteBuffer combinedData = BufferUtils.createByteBuffer(totalSize);
        int[] mipSizes = new int[output.size()];
        for (int i = 0; i < output.size(); i++) {
            ByteBuffer data = output.get(i);
            data.clear();
            scaleBias(alphaMult, 0, alphaChannel, data);
            data.clear();
            combinedData.put(data);
            mipSizes[i] = data.capacity();
        }
        combinedData.flip();

        // insert mip data into image
        image.setData(0, combinedData);
        image.setMipMapSizes(mipSizes);
        image.setFormat(format);
    }
    
    protected static void scaleBias(float scale, float bias, int alphaChannel, ByteBuffer data) {
        // Scale alpha channel.
        int count = data.capacity()/4;
        for(int i = 0; i < count; i++){
            float val = byteToFloat(data.get(i*4 + alphaChannel));
            val = FastMath.clamp(scale*val + bias, 0f, 1f);
            data.put(4*i + alphaChannel,floatToByte(val));
        }
    }
    
    // Byte to a float between 0 and 1.
    protected static float byteToFloat(byte b) {
        return (b & 0xFF) * 0.0039215686f;
    }

    // 0 to 1 float transformed into a byte value.
    protected static byte floatToByte(float f) {
        return (byte) (f * 255);
    }
}
