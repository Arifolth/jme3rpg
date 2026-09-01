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
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import ru.arifolth.anjrpg.interfaces.Constants;
import ru.arifolth.anjrpg.interfaces.bars.ManaBarInterface;
import ru.arifolth.anjrpg.interfaces.bars.StaminaBarInterface;
import ru.arifolth.anjrpg.models.PlayerCharacter;

public class PlayerStaminaBar extends AbstractPlayerBar implements StaminaBarInterface {

    public PlayerStaminaBar(AssetManager assetManager, PlayerCharacter character, SimpleApplication app) {
        super(assetManager, character, app);

        colorRGBA = ColorRGBA.Green;

        barContainer = new Node("PlayerStaminaBarContainer");
        barBackgroundName = "PlayerStaminaBarBackground";
        barFillName = "PlayerStaminaBarFill";
        barFrameName = "PlayerStaminaBarFrame";
    }

    @Override
    protected void initValues() {
        character.setStamina(character.getStats().getMaxStamina());
    }

    @Override
    protected void positionContainer() {
        //top margin + health bar + spacing + mana bar + spacing below mana bar
        float yPosition = screenHeight - Constants.PLAYER_BARS_TOP_MARGIN -
                barHeight - Constants.PLAYER_BARS_SPACING * 4 -
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
        float current = character.getStamina();
        float max = character.getStats().getMaxStamina();
        if (current < max) {
            float regenerationPerFrame = Constants.STAMINA_REGENERATION_RATE * 0.016f;
            float newStamina = Math.min(max, current + regenerationPerFrame);
            character.setStamina(newStamina);
            float staminaPercentage = newStamina / max;
            barFill.setLocalScale(staminaPercentage, 1f, 1f);
        }
    }

    @Override
    public void consumeStamina(float amount) {
        float current = character.getStamina();
        character.setStamina(Math.max(0, current - amount));
    }

    @Override
    public void restoreStamina(float amount) {
        float current = character.getStamina();
        float max = character.getStats().getMaxStamina();
        character.setStamina(Math.min(max, current + amount));
    }

    @Override
    public float getStamina() {
        return character.getStamina();
    }

    @Override
    public float getMaxStamina() {
        return character.getStats().getMaxStamina();
    }

    @Override
    public boolean isExhausted() {
        return character.getStamina() <= 0;
    }
}
