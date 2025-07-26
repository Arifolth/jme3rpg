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

package ru.arifolth.anjrpg;

import com.jme3.cursors.plugins.JmeCursor;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class MouseUtils {
    private MouseUtils() {
    }

    // format conversion method
    private static Image convertToRGBA8(Image original) {
        int width = original.getWidth();
        int height = original.getHeight();
        ByteBuffer srcBuf = original.getData(0);
        srcBuf.rewind();

        // Create destination buffer
        ByteBuffer dstBuf = BufferUtils.createByteBuffer(width * height * 4);

        // Convert based on original format
        switch (original.getFormat()) {
            case ABGR8:
                // ABGR -> RGBA conversion
                while (srcBuf.hasRemaining()) {
                    byte a = srcBuf.get();
                    byte b = srcBuf.get();
                    byte g = srcBuf.get();
                    byte r = srcBuf.get();
                    dstBuf.put(r).put(g).put(b).put(a);
                }
                break;

            case BGRA8:
                // BGRA -> RGBA conversion
                while (srcBuf.hasRemaining()) {
                    byte b = srcBuf.get();
                    byte g = srcBuf.get();
                    byte r = srcBuf.get();
                    byte a = srcBuf.get();
                    dstBuf.put(r).put(g).put(b).put(a);
                }
                break;

            case ARGB8:
                // ARGB -> RGBA conversion
                while (srcBuf.hasRemaining()) {
                    byte a = srcBuf.get();
                    byte r = srcBuf.get();
                    byte g = srcBuf.get();
                    byte b = srcBuf.get();
                    dstBuf.put(r).put(g).put(b).put(a);
                }
                break;

            default:
                throw new IllegalStateException("Unsupported cursor format: " + original.getFormat());
        }

        dstBuf.rewind();

        return new Image(Image.Format.RGBA8, width, height, dstBuf, null, ColorSpace.Linear);
    }

    public static JmeCursor getJmeCursor(Texture cursorTexture) {
        Image image = cursorTexture.getImage();

        // Convert to RGBA8 if needed
        if (image.getFormat() != Image.Format.RGBA8) {
            image = convertToRGBA8(image);
        }

        ByteBuffer imgByteBuff = image.getData(0);
        imgByteBuff.rewind();
        IntBuffer cursorData = BufferUtils.createIntBuffer(image.getHeight() * image.getWidth());

        // Proper RGBA to ARGB conversion with correct bit ordering
        while (imgByteBuff.hasRemaining()) {
            int r = imgByteBuff.get() & 0xFF;
            int g = imgByteBuff.get() & 0xFF;
            int b = imgByteBuff.get() & 0xFF;
            int a = imgByteBuff.get() & 0xFF;

            // binary alpha for compatibility
            // LWJGL cursors only guarantee 1-bit transparency support
            if (a < 128) {
                a = 0;  // Fully transparent
            } else {
                a = 255; // Fully opaque
            }

            // Pack into ARGB format: 0xAARRGGBB (correct bit ordering)
            int argb = (a << 24) | (r << 16) | (g << 8) | b;
            cursorData.put(argb);
        }

        cursorData.rewind();

        // Create cursor with proper configuration
        JmeCursor customCursor = new JmeCursor();
        customCursor.setWidth(image.getWidth());
        customCursor.setHeight(image.getHeight());
        customCursor.setNumImages(1);
        customCursor.setImagesData(cursorData);
        customCursor.setxHotSpot(0);
        customCursor.setyHotSpot(image.getHeight() - 1);

        return customCursor;
    }
}
