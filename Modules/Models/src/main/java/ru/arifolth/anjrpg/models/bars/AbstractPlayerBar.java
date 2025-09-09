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

package ru.arifolth.anjrpg.models.bars;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import ru.arifolth.anjrpg.interfaces.Constants;
import ru.arifolth.anjrpg.interfaces.bars.BarInterface;
import ru.arifolth.anjrpg.models.PlayerCharacter;

public abstract class AbstractPlayerBar implements BarInterface {
    protected final AssetManager assetManager;
    protected final PlayerCharacter character;
    protected final SimpleApplication app;

    protected Node barContainer;
    protected Geometry barBackground;
    protected Geometry barFill;
    protected ColorRGBA colorRGBA;
    private Geometry barFrame;
    protected String barBackgroundName;
    protected String barFillName;
    protected String barFrameName;
    protected float barWidth;
    protected float barHeight;
    protected int screenWidth;
    protected int screenHeight;

    protected Material backgroundMaterial;
    protected Material fillMaterial;

    public AbstractPlayerBar(AssetManager assetManager, PlayerCharacter character, SimpleApplication app) {
        this.assetManager = assetManager;
        this.character = character;
        this.app = app;

        initBarWidthHeight();
    }

    private void initBarWidthHeight() {
        screenWidth = app.getCamera().getWidth();
        screenHeight = app.getCamera().getHeight();

        barWidth = screenWidth * 0.325f;
        barHeight = screenHeight * 0.0065f;
    }

    @Override
    public void init() {
        initValues();

        createFrame();
        createBackground();
        createFill();
        positionContainer();
    }

    protected void createFrame() {
        float frameWidth = barWidth + (Constants.FRAME_BORDER_WIDTH * 2);
        float frameHeight = barHeight + (Constants.FRAME_BORDER_WIDTH * 2);

        Quad frameQuad = new Quad(frameWidth, frameHeight);
        barFrame = new Geometry(barFrameName, frameQuad);

        Material frameMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");

        // Use the same frame texture as compass
        Texture borderTexture = assetManager.loadTexture("Textures/Compass/compass_brass_wheel3.png");
        frameMaterial.setTexture("ColorMap", borderTexture);

        barFrame.setMaterial(frameMaterial);
        barFrame.setLocalTranslation(-Constants.FRAME_BORDER_WIDTH, -Constants.FRAME_BORDER_WIDTH, 0f);

        barContainer.attachChild(barFrame);
    }

    protected void createBackground() {
        if (backgroundMaterial == null) {
            backgroundMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            backgroundMaterial.setColor("Color", new ColorRGBA(0.2f, 0.2f, 0.2f, 0.8f));
        }

        Quad backgroundQuad = new Quad(barWidth, barHeight);
        barBackground = new Geometry(barBackgroundName, backgroundQuad);
        barBackground.setMaterial(backgroundMaterial);

        barContainer.attachChild(barBackground);
    }

    protected void createFill() {
        Quad fillQuad = new Quad(barWidth, barHeight); // Full size quad
        barFill = new Geometry(barFillName, fillQuad);

        Material fillMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        fillMaterial.setColor("Color", colorRGBA);
        barFill.setMaterial(fillMaterial);
        barFill.setLocalTranslation(0, 0, 0.1f);

        barContainer.attachChild(barFill);
    }

    protected abstract void initValues();

    @Override
    public void create() {
        initValues();

        app.getGuiNode().attachChild(barContainer);
    }

    protected abstract void positionContainer();

    @Override
    public void destroy() {
        if (barContainer != null) {
            barContainer.removeFromParent();
        }
    }
}
