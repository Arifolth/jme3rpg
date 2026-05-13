/**
 * ANJRpg - an open source Role Playing Game written in Java.
 * Copyright (C) 2014 - 2026 Alexander Nilov
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package ru.arifolth.anjrpg.models.bars;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import ru.arifolth.anjrpg.interfaces.Constants;
import ru.arifolth.anjrpg.interfaces.bars.ManaBarInterface;
import ru.arifolth.anjrpg.models.PlayerCharacter;

public class PlayerManaBar extends AbstractPlayerBar implements ManaBarInterface {
    private float currentMana;

    public PlayerManaBar(AssetManager assetManager, PlayerCharacter character, SimpleApplication app) {
        super(assetManager, character, app);

        colorRGBA = ColorRGBA.Blue;

        barContainer = new Node("PlayerManaBarContainer");
        barBackgroundName = "PlayerManaBarBackground";
        barFillName = "PlayerManaBarFill";
        barFrameName = "PlayerManaBarFrame";
    }

    @Override
    protected void initValues() {
        currentMana = Constants.MAXIMUM_MANA;
    }

    @Override
    protected void positionContainer() {
        //top margin + health bar height + spacing below health bar
        float yPosition = screenHeight - Constants.PLAYER_BARS_TOP_MARGIN -
                barHeight - Constants.PLAYER_BARS_SPACING * 4 -
                barHeight;

        barContainer.setLocalTranslation(
                Constants.PLAYER_BARS_LEFT_MARGIN,
                yPosition,
                1f
        );
    }

    @Override
    public void update() {
        // Auto-regenerate mana
        float regenerationPerFrame = Constants.MANA_REGENERATION_RATE * 0.016f; // assuming ~60fps
        currentMana = Math.min(Constants.MAXIMUM_MANA, currentMana + regenerationPerFrame);

        float manaPercentage = currentMana / Constants.MAXIMUM_MANA;
        barFill.setLocalScale(manaPercentage, 1f, 1f);
    }

    @Override
    public void consumeMana(float amount) {
        currentMana = Math.max(0, currentMana - amount);
    }

    @Override
    public void restoreMana(float amount) {
        currentMana = Math.min(Constants.MAXIMUM_MANA, currentMana + amount);
    }

    @Override
    public float getMana() {
        return currentMana;
    }

    @Override
    public float getMaxMana() {
        return Constants.MAXIMUM_MANA;
    }
}
