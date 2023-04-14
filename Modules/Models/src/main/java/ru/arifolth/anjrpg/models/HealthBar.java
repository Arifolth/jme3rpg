/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2014 - 2023 Alexander Nilov
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

package ru.arifolth.anjrpg.models;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.shape.Quad;
import ru.arifolth.anjrpg.interfaces.HealthBarInterface;

public class HealthBar implements HealthBarInterface {
    public static final float MAXIMUM_HEALTH = 100f;
    public static final String HEALTH = "health";
    private final AssetManager assetManager;
    private final PlayerCharacter character;
    private Geometry healthbar;

    public HealthBar(AssetManager assetManager, PlayerCharacter character) {
        this.assetManager = assetManager;
        this.character = character;
    }

    @Override
    public void create() {
        character.setHealth(MAXIMUM_HEALTH);

        // add healthbar
        BillboardControl billboard = new BillboardControl();
        //new Quad(HEALTHBAR_LENGTH, HELTHBAR_HEIGHT))
        healthbar = new Geometry(this.getClass().getName(), new Quad(character.getHealth() / 25f, 0.2f));
        Material mathb = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mathb.setColor("Color", ColorRGBA.Red);
        healthbar.setMaterial(mathb);
        healthbar.setLocalTranslation(0.3f, 6.0f, 0f);
        healthbar.addControl(billboard);

        character.getNode().attachChild(healthbar);
    }

    @Override
    public void destroy() {
        character.getNode().detachChild(healthbar);
    }

    @Override
    public void update() {
        ((Quad) healthbar.getMesh()).updateGeometry(character.getHealth() / 25f, 0.2f);
    }

    @Override
    public void applyDamage(float delta) {
        character.setHealth(character.getHealth() - delta);

        character.setPlayerDamaged();
        if (getHealth() <= 0 && !character.isDead()) {
            character.die();
        }
    }

    @Override
    public float getHealth() {
        return character.getHealth() / 25f;
    }
}
