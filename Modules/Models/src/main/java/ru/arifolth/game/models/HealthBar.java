/**
 *     Copyright (C) 2021  Alexander Nilov
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

package ru.arifolth.game.models;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.shape.Quad;

import static ru.arifolth.game.models.PlayerCharacter.MAXIMUM_HEALTH;

public class HealthBar {
    private AssetManager assetManager;
    private Node characterNode;

    public HealthBar(AssetManager assetManager, Node characterNode) {
        this.assetManager = assetManager;
        this.characterNode = characterNode;
    }

    public void create() {
        characterNode.setUserData("health", MAXIMUM_HEALTH);

        // add healthbar
        BillboardControl billboard = new BillboardControl();
        //new Quad(HEALTHBAR_LENGTH, HELTHBAR_HEIGHT))
        Geometry healthbar = new Geometry("healthbar", new Quad((float) characterNode.getUserData("health") / 25f, 0.2f));
        Material mathb = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mathb.setColor("Color", ColorRGBA.Red);
        healthbar.setMaterial(mathb);
        healthbar.setLocalTranslation(0.3f, 6.0f, 0f);
        healthbar.addControl(billboard);

        characterNode.attachChild(healthbar);
    }

    public void update() {
        ((Quad)((Geometry)characterNode.getChild("healthbar")).getMesh()).updateGeometry((float) characterNode.getUserData("health") / 25f, 0.2f);
    }
}
