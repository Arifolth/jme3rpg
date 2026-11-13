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

package ru.arifolth.anjrpg.loading;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import ru.arifolth.anjrpg.interfaces.Constants;

/**
 * A progress bar for the loading screen, built after the visual pattern of
 * {@code AbstractPlayerBar} / {@code PlayerHealthBar}: a brass frame quad,
 * a dark background quad and a solid fill quad whose X scale tracks the
 * load progress. Not a player bar, so it does not implement
 * {@code BarInterface}.
 */
public class LoadingProgressBar {
    private final AssetManager assetManager;
    private final SimpleApplication app;

    private Node container;
    private Geometry fill;

    private final float barWidth;
    private final float barHeight;

    public LoadingProgressBar(AssetManager assetManager, SimpleApplication app) {
        this.assetManager = assetManager;
        this.app = app;
        this.barWidth  = app.getCamera().getWidth()  * 0.5f;
        this.barHeight = app.getCamera().getHeight() * 0.02f;
    }

    public void init() {
        container = new Node("LoadingProgressBar");

        float frameWidth  = barWidth  + Constants.FRAME_BORDER_WIDTH * 2;
        float frameHeight = barHeight + Constants.FRAME_BORDER_WIDTH * 2;

        Geometry frame = new Geometry("LoadingBarFrame", new Quad(frameWidth, frameHeight));
        Material frameMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        frameMat.setTexture("ColorMap",
                assetManager.loadTexture("Textures/Compass/compass_brass_wheel3.png"));
        frame.setMaterial(frameMat);
        frame.setLocalTranslation(-Constants.FRAME_BORDER_WIDTH,
                                  -Constants.FRAME_BORDER_WIDTH, 0f);
        container.attachChild(frame);

        Geometry background = new Geometry("LoadingBarBackground", new Quad(barWidth, barHeight));
        Material bgMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        bgMat.setColor("Color", new ColorRGBA(0.2f, 0.2f, 0.2f, 0.8f));
        background.setMaterial(bgMat);
        container.attachChild(background);

        fill = new Geometry("LoadingBarFill", new Quad(barWidth, barHeight));
        Material fillMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        fillMat.setColor("Color", ColorRGBA.White);
        fill.setMaterial(fillMat);
        fill.setLocalTranslation(0, 0, 0.1f);
        container.attachChild(fill);

        positionContainer();
    }

    private void positionContainer() {
        int screenWidth  = app.getCamera().getWidth();
        int screenHeight = app.getCamera().getHeight();
        // Bottom third of the screen, horizontally centered.
        float x = (screenWidth - barWidth) * 0.5f;
        float y = screenHeight / 3f * 0.5f;
        container.setLocalTranslation(x, y, 1f);
    }

    public Node getContainer() {
        return container;
    }

    public void setProgress(float fraction) {
        float f = Math.max(0f, Math.min(1f, fraction));
        fill.setLocalScale(f, 1f, 1f);
    }
}
