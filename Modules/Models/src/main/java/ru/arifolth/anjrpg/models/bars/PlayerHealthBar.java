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
import ru.arifolth.anjrpg.interfaces.bars.HealthBarInterface;
import ru.arifolth.anjrpg.models.PlayerCharacter;

public class PlayerHealthBar extends AbstractPlayerBar implements HealthBarInterface {

    public PlayerHealthBar(AssetManager assetManager, PlayerCharacter character, SimpleApplication app) {
        super(assetManager, character, app);

        colorRGBA = ColorRGBA.Red;

        barContainer = new Node("PlayerHealthBarContainer");
        barBackgroundName = "PlayerHealthBarBackground";
        barFillName = "PlayerHealthBarFill";
        barFrameName = "PlayerHealthBarFrame";
    }

    @Override
    protected void initValues() {
        character.setHealth(Constants.MAXIMUM_HEALTH);
    }

    @Override
    protected void positionContainer() {
        float yPosition = screenHeight - Constants.PLAYER_BARS_TOP_MARGIN - barHeight;

        barContainer.setLocalTranslation(
                Constants.PLAYER_BARS_LEFT_MARGIN,
                yPosition,
                1f
        );
    }

    @Override
    public void update() {
        float healthPercentage = character.getHealth() / Constants.MAXIMUM_HEALTH;
        barFill.setLocalScale(healthPercentage, 1f, 1f);
    }

    @Override
    public void applyDamage(float delta) {
        character.setHealth(Math.max(0, character.getHealth() - delta));
        character.setPlayerDamaged();

        if (getHealth() <= 0 && !character.isDead()) {
            character.die();
        }
    }

    @Override
    public float getHealth() {
        return character.getHealth();
    }
}
