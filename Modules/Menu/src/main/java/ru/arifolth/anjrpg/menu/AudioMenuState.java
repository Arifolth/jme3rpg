/**
 ANJRpg - an open source Role Playing Game written in Java.
 Copyright (C) 2014 - 2026 Alexander Nilov
 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package ru.arifolth.anjrpg.menu;

import com.jme3.app.Application;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.component.DynamicInsetsComponent;
import com.simsilica.lemur.component.SpringGridLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.arifolth.anjrpg.interfaces.ANJRpgInterface;
import ru.arifolth.anjrpg.interfaces.GameLogicCoreInterface;
import ru.arifolth.anjrpg.interfaces.RolePlayingGameInterface;
import ru.arifolth.anjrpg.interfaces.SoundManagerInterface;
import ru.arifolth.anjrpg.interfaces.SoundTypeEnum;

import static com.simsilica.lemur.component.BorderLayout.Position.East;
import static com.simsilica.lemur.component.BorderLayout.Position.West;

public class AudioMenuState extends CustomCompositeAppState {
    private final static Logger LOGGER = LoggerFactory.getLogger(AudioMenuState.class);

    private final SoundManagerInterface soundManager;
    private RangedValueModel volumeModel = new DefaultRangedValueModel(0, 1, 0.5);
    private ANJRpgInterface application;
    private GameLogicCoreInterface gameLogicCore;

    // FIX: Flag to prevent onDisable() from re-enabling the parent menu
    // when we are intentionally navigating all the way back to the Main Menu.
    private boolean isApplying = false;

    public AudioMenuState(OptionsMenuState parent) {
        super(parent);
        this.soundManager = ((RolePlayingGameInterface) this.parent.getApplication()).getSoundManager();
    }

    private void apply() {
        gameLogicCore.getSoundManager().getSoundNode(SoundTypeEnum.MENU).play();

        // Apply audio settings
        soundManager.setVolume((float) volumeModel.getValue());
        soundManager.reInitialize(gameLogicCore);
        gameLogicCore.reInitialize();

        // FIX: Set flag before navigation to prevent race condition in onDisable()
        isApplying = true;

        // Disable the parent (OptionsMenuState). This will trigger its onDisable(),
        // which correctly enables the MainMenuState.
        parent.setEnabled(false);

        // FIX: Detach this state from the StateManager so it doesn't linger
        // and cause input conflicts on the second attempt.
        getStateManager().detach(this);
    }

    @Override
    protected void initialize(Application app) {
        application = (ANJRpgInterface) app;
        gameLogicCore = application.getGameLogicCore();
    }

    @Override
    protected void cleanup(Application app) {
        getState(MainMenuState.class).setEnabled(true);
    }

    @Override
    protected void onEnable() {
        window = new Container();
        parent.getMainWindow().clearChildren();

        Container contentContainer = new Container(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.Even));
        contentContainer.setBackground(null);
        Container menuContainer = window.addChild(contentContainer);

        Label title = menuContainer.addChild(new Label("Audio"));
        title.setFontSize(24);
        title.setInsets(new Insets3f(10, 10, 0, 10));

        Container props;
        Container container = new Container();
        container.setBackground(null);
        Container joinPanel = menuContainer.addChild(container);
        joinPanel.setInsets(new Insets3f(10, 10, 10, 10));

        props = joinPanel.addChild(new Container(new BorderLayout()));
        props.setBackground(null);
        props.addChild(new Label("Audio Volume:"), West);

        Slider slider = props.addChild(new Slider(volumeModel), East);
        slider.getDecrementButton().addClickCommands();
        slider.setInsetsComponent(new DynamicInsetsComponent(0.5f, 0.5f, 0.5f, 0.5f));

        props = joinPanel.addChild(new Container(new BorderLayout()));
        props.setBackground(null);
        props.addChild(new ActionButton(new CallMethodAction("Apply", this, "apply")), West);
        props.addChild(new ActionButton(new CallMethodAction("Back", this, "onDisable")), East);

        window.setBackground(null);
        parent.getMainWindow().addChild(window, East);
        GuiGlobals.getInstance().requestFocus(window);
    }

    @Override
    protected void onDisable() {
        gameLogicCore.getSoundManager().getSoundNode(SoundTypeEnum.MENU).play();

        if (window != null) {
            window.removeFromParent();
        }

        // FIX: Only navigate back to the parent menu if we are NOT applying settings.
        // If we are applying, parent.setEnabled(false) was already called in apply(),
        // which handles the navigation to the Main Menu. Calling parent.onEnable() here
        // would incorrectly re-open the Options menu.
        if (!isApplying) {
            parent.onEnable();
        }
    }
}