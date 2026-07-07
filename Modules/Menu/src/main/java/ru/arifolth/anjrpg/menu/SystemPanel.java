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
import com.jme3.math.Vector3f;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.component.SpringGridLayout;
import ru.arifolth.anjrpg.interfaces.ANJRpgInterface;
import ru.arifolth.anjrpg.interfaces.SoundTypeEnum;

import java.util.HashMap;
import java.util.Map;

public class SystemPanel implements MenuPanel {
    private Container container;
    private Container optionsListPanel;
    private Container subDetailPanel;
    private MenuPanel currentSubPanel;
    private final Map<String, MenuPanel> subPanels = new HashMap<>();
    private Application app;
    private InGameMenuState parentMenu;

    // Stores the name of the last active sub-panel to restore it after a restart
    private String lastSubPanel;

    public SystemPanel(Application app, InGameMenuState parentMenu, String initialSubPanel) {
        this.app = app;
        this.parentMenu = parentMenu;
        this.lastSubPanel = initialSubPanel;

        // Register sub-panels
        subPanels.put("Video", new VideoPanel(app, this));
        subPanels.put("Audio", new AudioPanel(app, this));
        subPanels.put("Controls", new ControlsPanel(app, this));
        subPanels.put("Gameplay", new GameplayPanel(app, this));

        // Main container uses BorderLayout to split into options list and detail area
        container = GameUI.createPanelContainer();
        container.setLayout(new BorderLayout());

        // --- LEFT: Options buttons list ---
        optionsListPanel = GameUI.createPanelContainer();
        optionsListPanel.setLayout(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.None));
        optionsListPanel.setPreferredSize(new Vector3f(200, 0, 0));

        Label title = optionsListPanel.addChild(new Label("Options"));
        title.setFontSize(32);
        title.setInsets(new Insets3f(10, 10, 0, 10));

        ActionButton video = optionsListPanel.addChild(new ActionButton(new CallMethodAction("Video", this, "video")));
        video.setInsets(new Insets3f(10, 10, 10, 10));

        ActionButton audio = optionsListPanel.addChild(new ActionButton(new CallMethodAction("Audio", this, "audio")));
        audio.setInsets(new Insets3f(10, 10, 10, 10));

        ActionButton controls = optionsListPanel.addChild(new ActionButton(new CallMethodAction("Controls", this, "controls")));
        controls.setInsets(new Insets3f(10, 10, 10, 10));

        ActionButton gameplay = optionsListPanel.addChild(new ActionButton(new CallMethodAction("Gameplay", this, "gameplay")));
        gameplay.setInsets(new Insets3f(10, 10, 10, 10));

        container.addChild(optionsListPanel, BorderLayout.Position.West);

        // --- RIGHT: Sub-detail area for selected option ---
        subDetailPanel = GameUI.createPanelContainer();
        subDetailPanel.setLayout(new BorderLayout());
        container.addChild(subDetailPanel, BorderLayout.Position.Center);

        // Automatically open the last active sub-panel if one exists
        if (lastSubPanel != null) {
            showSubPanel(lastSubPanel);
        }
    }

    @Override
    public Container getContainer() {
        return container;
    }

    public void video() {
        playMenuSound();
        showSubPanel("Video");
    }

    public void audio() {
        playMenuSound();
        showSubPanel("Audio");
    }

    public void controls() {
        playMenuSound();
        showSubPanel("Controls");
    }

    public void gameplay() {
        playMenuSound();
        showSubPanel("Gameplay");
    }

    public void resumeGame() {
        playMenuSound();
        parentMenu.setEnabled(false);
    }

    /**
     * Called by sub-panels before closing the menu to remember state
     */
    public void setLastSubPanel(String name) {
        this.lastSubPanel = name;
        // Also save it to the InGameMenuState so it survives a restart
        parentMenu.setLastSystemSubPanel(name);
    }

    /**
     * Swaps the content in the right-side sub-detail area.
     */
    public void showSubPanel(String name) {
        if (currentSubPanel != null) {
            subDetailPanel.removeChild(currentSubPanel.getContainer());
        }

        currentSubPanel = subPanels.get(name);
        if (currentSubPanel != null) {
            subDetailPanel.addChild(currentSubPanel.getContainer(), BorderLayout.Position.Center);
        }
    }

    /**
     * Clears the right-side sub-detail area.
     */
    public void clearSubPanel() {
        if (currentSubPanel != null) {
            subDetailPanel.removeChild(currentSubPanel.getContainer());
            currentSubPanel = null;
        }
    }

    @Override
    public void onDeactivated() {
        clearSubPanel();
    }

    private void playMenuSound() {
        ((ANJRpgInterface) app).getGameLogicCore().getSoundManager().getSoundNode(SoundTypeEnum.MENU).play();
    }
}