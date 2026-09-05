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
import com.jme3.system.AppSettings;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.component.SpringGridLayout;
import ru.arifolth.anjrpg.interfaces.ANJRpgInterface;
import ru.arifolth.anjrpg.interfaces.Constants;
import ru.arifolth.anjrpg.interfaces.SoundTypeEnum;

import static com.simsilica.lemur.component.BorderLayout.Position.East;
import static com.simsilica.lemur.component.BorderLayout.Position.West;

public class GameplayPanel implements MenuPanel {
    private final Application application;
    private Container container;
    private SystemPanel parentPanel;

    private Checkbox debugCheckbox;

    public GameplayPanel(Application app, SystemPanel parentPanel) {
        this.application = app;
        this.parentPanel = parentPanel;

        // Initialize checkbox with current settings
        AppSettings settings = application.getContext().getSettings();
        debugCheckbox = new Checkbox(Constants.DEBUG);
        debugCheckbox.setChecked(settings.getBoolean(Constants.DEBUG));

        container = GameUI.createPanelContainer();
        container.setLayout(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.None));

        Label title = container.addChild(new Label("Gameplay"));
        title.setFontSize(32);
        title.setInsets(new Insets3f(10, 10, 0, 10));

        buildUI();
    }

    @Override
    public void buildUI() {
        Container props;
        Container panelContainer = new Container();
        panelContainer.setBackground(null);
        Container joinPanel = container.addChild(panelContainer);
        joinPanel.setInsets(new Insets3f(10, 10, 10, 10));

        joinPanel.addChild(debugCheckbox);
        debugCheckbox.setInsets(new Insets3f(10, 20, 10, 20));

        props = joinPanel.addChild(new Container(new BorderLayout()));
        props.setBackground(null);
        props.addChild(new ActionButton(new CallMethodAction("Apply", this, "apply")), West);

        // Add Back button only for Main Menu context (when parentPanel is null)
        if (parentPanel == null) {
            props.addChild(new ActionButton(new CallMethodAction("Back", this, "back")), East);
        }
    }

    @Override
    public Container getContainer() {
        return container;
    }

    private void apply() {
        ((ANJRpgInterface) application).getSoundManager().getSoundNode(SoundTypeEnum.MENU).play();

        AppSettings settings = application.getContext().getSettings();
        settings.putBoolean(Constants.DEBUG, debugCheckbox.isChecked());

        SettingsUtils.saveSettings(settings);
        application.getContext().setSettings(settings);

        if (parentPanel != null) {
            // In-Game Context
            parentPanel.setLastSubPanel("Gameplay");
            InGameMenuState inGameMenu = (InGameMenuState) application.getStateManager().getState(InGameMenuState.class);
            if(inGameMenu == null)
                return;
            inGameMenu.setEnabled(false);
            application.getContext().restart();
            inGameMenu.setEnabled(true);
        } else {
            // Main Menu Context: Directly disable OptionsMenuState to return to MainMenu root
            OptionsMenuState optionsMenu = (OptionsMenuState) application.getStateManager().getState(OptionsMenuState.class);
            if(optionsMenu == null)
                return;
            optionsMenu.setEnabled(false);
            application.getContext().restart();
            optionsMenu.setEnabled(true);
        }
    }

    private void back() {
        ((ANJRpgInterface) application).getSoundManager().getSoundNode(SoundTypeEnum.MENU).play();
        if (parentPanel == null) {
            // Main Menu Context: Directly disable OptionsMenuState to return to MainMenu root
            OptionsMenuState optionsMenu = (OptionsMenuState) application.getStateManager().getState(OptionsMenuState.class);
            if (optionsMenu != null) optionsMenu.setEnabled(false);
        }
    }
}