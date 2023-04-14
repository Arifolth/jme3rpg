/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2014 - 2023 Alexander Nilov
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.arifolth.anjrpg.interfaces.ANJRpgInterface;
import ru.arifolth.anjrpg.interfaces.Constants;
import ru.arifolth.anjrpg.interfaces.GameLogicCoreInterface;
import ru.arifolth.anjrpg.interfaces.SoundType;

import static com.simsilica.lemur.component.BorderLayout.Position.East;
import static com.simsilica.lemur.component.BorderLayout.Position.West;

public class GamePlayMenuState extends CustomCompositeAppState {
    private final static Logger LOGGER = LoggerFactory.getLogger(OptionsMenuState.class);
    private ANJRpgInterface application;
    private GameLogicCoreInterface gameLogicCore;
    private Checkbox debugCheckbox = new Checkbox(Constants.DEBUG);

    public GamePlayMenuState(OptionsMenuState parent) {
        super(parent);
    }

    @Override
    protected void initialize(Application app) {
        application = (ANJRpgInterface) app;
        gameLogicCore = application.getGameLogicCore();

        AppSettings settings = application.getContext().getSettings();
        debugCheckbox.setChecked(settings.getBoolean(Constants.DEBUG));
    }

    @Override
    protected void cleanup(Application app) {
        getState(MainMenuState.class).setEnabled(true);
    }

    private void apply() {
        AppSettings settings = application.getSettings();
        applySettings(settings);
        getApplication().setSettings(settings);

        setEnabled(false);
        parent.setEnabled(false);

        SettingsUtils.saveSettings(settings);

        getApplication().getContext().setSettings(settings);
        getApplication().getContext().restart();
    }

    private void applySettings(AppSettings settings) {
        settings.putBoolean(Constants.DEBUG, debugCheckbox.isChecked());
    }

    @Override
    protected void onEnable() {
        window = new Container();

        Container menuContainer = window.addChild(new Container(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.Even)));
        Label title = menuContainer.addChild(new Label("Video"));
        title.setFontSize(24);
        title.setInsets(new Insets3f(10, 10, 0, 10));

        Container props;
        Container joinPanel = menuContainer.addChild(new Container());
        joinPanel.setInsets(new Insets3f(10, 10, 10, 10));

        Checkbox debugChkbx = joinPanel.addChild(debugCheckbox);

        props = joinPanel.addChild(new Container(new BorderLayout()));
        props.setBackground(null);
        props.addChild(new ActionButton(new CallMethodAction("Apply", this, "apply")), West);

        parent.getMainWindow().addChild(window, East);
        GuiGlobals.getInstance().requestFocus(window);
    }

    @Override
    protected void onDisable() {
        window.removeFromParent();
    }

    public OptionsMenuState getParent() {
        return this.parent;
    }
}
