/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2021 Alexander Nilov
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

import com.jme3.ui.Picture;
import ru.arifolth.anjrpg.weather.Emitter;
import ru.arifolth.anjrpg.weather.RainEmitter;
import ru.arifolth.game.models.NonPlayerCharacter;
import ru.arifolth.game.models.PlayerCharacter;

public class Initializer {
    public static final int NPC_AMOUNT = 1;
    private final GameLogicCore gameLogicCore;

    public Initializer(GameLogicCore gameLogicCore) {
        this.gameLogicCore = gameLogicCore;
    }

    void setupDamageIndicator() {
        Picture damageIndicator = new Picture("DamageIndicator");
        damageIndicator.setImage(gameLogicCore.getAssetManager(), "Textures/damageIndicator.png", true);
        damageIndicator.setWidth(((ANJRpg)gameLogicCore.getApp()).getSettings().getWidth());
        damageIndicator.setHeight(((ANJRpg)gameLogicCore.getApp()).getSettings().getHeight());
        damageIndicator.setPosition(0, 0);

        gameLogicCore.setDamageIndicator(damageIndicator);
    }

    void setupNPC() {
        for (int i = 0; i < NPC_AMOUNT; i++) {
            NonPlayerCharacter nonPlayerCharacter = (NonPlayerCharacter) gameLogicCore.getCharacterFactory().createCharacter(NonPlayerCharacter.class);
            nonPlayerCharacter.setPlayerCharacter(gameLogicCore.getPlayerCharacter());
            gameLogicCore.getCharacterMap().put(nonPlayerCharacter.getNode(), nonPlayerCharacter);
        }
    }

    void setupPlayer() {
        //create player
        PlayerCharacter playerCharacter = (PlayerCharacter) gameLogicCore.getCharacterFactory().createCharacter(PlayerCharacter.class);
        playerCharacter.setCam(gameLogicCore.getCam());
        playerCharacter.setDamageIndicator(gameLogicCore.getDamageIndicator());
        gameLogicCore.setPlayerCharacter(playerCharacter);
        gameLogicCore.getMovementController().setPlayerCharacter(playerCharacter);
    }

    void setupWeatherEffects() {
        Emitter emitter = new RainEmitter(gameLogicCore.getRootNode(), gameLogicCore.getAssetManager());
        emitter.setSpatial(gameLogicCore.getPlayerCharacter().getNode());
        gameLogicCore.getWeatherEffectsSet().add(emitter);
    }
}