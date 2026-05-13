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

package ru.arifolth.anjrpg.weather;

import com.jme3.audio.AudioNode;
import ru.arifolth.anjrpg.interfaces.*;
import ru.arifolth.anjrpg.interfaces.weather.EmitterInterface;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WeatherManager implements WeatherManagerInterface {
    final private static Logger LOGGER = Logger.getLogger(WeatherManager.class.getName());

    public static final float DAYTPF = 40f;
    private final GameLogicCoreInterface gameLogicCore;
    private Set<EmitterInterface> weatherEffectsSet = new LinkedHashSet<>();

    private final int PROBABILITY = 25;

    private boolean mist = false;
    private float dayTpf = DAYTPF;
    private AudioNode audioNode;


    public WeatherManager(GameLogicCoreInterface gameLogicCore) {
        this.gameLogicCore = gameLogicCore;

        audioNode = gameLogicCore.getSoundManager().getSoundNode(SoundTypeEnum.RAIN);
        audioNode.setName(SoundTypeEnum.RAIN.name());
    }

    @Override
    public void update(float tpf) {
        try {
            if(!weatherEffectsSet.isEmpty()) {
                //raining
                for (EmitterInterface emitter : weatherEffectsSet) {
                    emitter.update(tpf);
                }
            }
        } finally {
            if (dayTpf >= 0) {
                dayTpf -= tpf;
            } else {
                dayTpf = DAYTPF;
                LOGGER.log(Level.INFO, "RAIN STOPS");
                detachRain();

                if (isRoll() && weatherEffectsSet.isEmpty()) {
                    attachRain();

                    LOGGER.log(Level.INFO, "ITS RAINING!");
                }
            }
        }
    }

    @Override
    public void attachRain() {
        EmitterInterface emitter = new RainEmitter(gameLogicCore.getRootNode(), gameLogicCore.getAssetManager());
        emitter.setSpatial(gameLogicCore.getPlayerCharacter().getNode());
        weatherEffectsSet.add(emitter);

        gameLogicCore.getPlayerCharacter().getNode().attachChild(audioNode);

        audioNode.play();

        ((RolePlayingGameInterface) gameLogicCore.getApp()).getFilterManager().addFog();
    }

    @Override
    public void detachRain() {
        weatherEffectsSet.clear();

        audioNode.stop();
        gameLogicCore.getPlayerCharacter().getNode().detachChildNamed(SoundTypeEnum.RAIN.name());

        ((RolePlayingGameInterface) gameLogicCore.getApp()).getFilterManager().removeFog();
    }

    @Override
    public boolean isRoll() {
        boolean probability = Utils.getRandom(PROBABILITY);

        LOGGER.log(Level.INFO, "WEATHER ROLL: " + probability);

        return probability;
    }

}
