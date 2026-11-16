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

package ru.arifolth.anjrpg.menu;

import com.jme3.app.Application;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.GuiGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.arifolth.anjrpg.interfaces.ANJRpgInterface;
import ru.arifolth.anjrpg.interfaces.GameLogicCoreInterface;
import ru.arifolth.anjrpg.interfaces.SoundTypeEnum;

import static com.simsilica.lemur.component.BorderLayout.Position.East;

/**
 * Main-menu wrapper for {@link AdvancedVideoPanel}, mirroring
 * {@link VideoMenuState}: the panel is created with a null SystemPanel, so
 * it shows the Back button and its Apply only persists the filter toggles
 * (the world - and with it the FilterManager - is not loaded yet; the
 * values are applied when the filters are created).
 */
public class AdvancedVideoMenuState extends CustomCompositeAppState {
    private final static Logger LOGGER = LoggerFactory.getLogger(AdvancedVideoMenuState.class);
    private ANJRpgInterface application;
    private GameLogicCoreInterface gameLogicCore;
    private AdvancedVideoPanel advancedVideoPanel;

    public AdvancedVideoMenuState(OptionsMenuState parent) {
        super(parent);
    }

    public OptionsMenuState getParent() {
        return parent;
    }

    @Override
    protected void initialize(Application app) {
        application = (ANJRpgInterface) app;
        gameLogicCore = application.getGameLogicCore();

        // Instantiate the panel. Pass null for SystemPanel since this is used in the MainMenu.
        advancedVideoPanel = new AdvancedVideoPanel(application, null);
    }

    @Override
    protected void onEnable() {
        window = new Container();
        parent.getMainWindow().clearChildren();

        // Reuse the AdvancedVideoPanel's container directly, eliminating layout duplication
        Container contentContainer = advancedVideoPanel.getContainer();
        contentContainer.setBackground(null);

        window.addChild(contentContainer);
        window.setBackground(null);

        parent.getMainWindow().addChild(window, East);
        GuiGlobals.getInstance().requestFocus(window);
    }

    @Override
    protected void onDisable() {
        gameLogicCore.getSoundManager().getSoundNode(SoundTypeEnum.MENU).play();
        window.removeFromParent();

        // Directly disable the OptionsMenuState to return to the Main Menu root.
        if (parent != null) {
            parent.setEnabled(false);
        }
    }
}
