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

package ru.arifolth.anjrpg.bars;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import ru.arifolth.anjrpg.interfaces.ANJRpgInterface;
import ru.arifolth.anjrpg.models.PlayerCharacter;
import ru.arifolth.anjrpg.models.bars.PlayerHealthBar;
import ru.arifolth.anjrpg.models.bars.PlayerManaBar;
import ru.arifolth.anjrpg.models.bars.PlayerStaminaBar;

import java.util.logging.Logger;

public class PlayerBarsState extends BaseAppState {
    final private static Logger LOGGER = Logger.getLogger(PlayerBarsState.class.getName());

    private PlayerCharacter playerCharacter;
    private PlayerHealthBar healthBar;
    private PlayerManaBar manaBar;
    private PlayerStaminaBar staminaBar;
    private boolean barsVisible = false;

    @Override
    protected void initialize(Application app) {
        SimpleApplication application = (SimpleApplication) app;
        this.playerCharacter = (PlayerCharacter) ((ANJRpgInterface) application).getGameLogicCore().getPlayerCharacter();

        // Initialize bars
        healthBar = new PlayerHealthBar(application.getAssetManager(), playerCharacter, application);
        healthBar.init();

        manaBar = new PlayerManaBar(application.getAssetManager(), playerCharacter, application);
        manaBar.init();

        staminaBar = new PlayerStaminaBar(application.getAssetManager(), playerCharacter, application);
        staminaBar.init();

        // Set the bars in the player character
        playerCharacter.setHealthBar(healthBar);
        playerCharacter.setManaBar(manaBar);
        playerCharacter.setStaminaBar(staminaBar);
    }

    @Override
    public void update(float tpf) {
        // Check if player just spawned and bars need to be shown
        if (!playerCharacter.isDead() && !barsVisible) {
            showBars();
        }
        // Check if player just died and bars need to be hidden
        else if (playerCharacter.isDead() && barsVisible) {
            hideBars();
        }

        // Update bars if they're visible
        if (barsVisible) {
            healthBar.update();
            manaBar.update();
            staminaBar.update();
        }
    }

    /**
     * Show/create the bars on screen
     */
    public void showBars() {
        if (!barsVisible) {
            healthBar.create();
            manaBar.create();
            staminaBar.create();

            barsVisible = true;
        }
    }

    /**
     * Hide/destroy the bars from screen
     */
    public void hideBars() {
        if (barsVisible) {
            healthBar.destroy();
            manaBar.destroy();
            staminaBar.destroy();

            barsVisible = false;
        }
    }

    @Override
    protected void cleanup(Application app) {
        if (barsVisible) {
            hideBars();
        }
    }

    @Override
    protected void onEnable() {
        // Show bars when state is enabled (initial game start)
        if (!playerCharacter.isDead()) {
            showBars();
        }
    }

    @Override
    protected void onDisable() {
        // Hide bars when state is disabled
        if (barsVisible) {
            hideBars();
        }
    }
}
