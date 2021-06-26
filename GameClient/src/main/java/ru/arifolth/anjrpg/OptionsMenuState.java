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

import com.jme3.app.Application;
import com.jme3.app.state.AppState;
import com.jme3.app.state.BaseAppState;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.state.CompositeAppState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static com.simsilica.lemur.component.BorderLayout.Position.East;
import static com.simsilica.lemur.component.BorderLayout.Position.West;

public class OptionsMenuState extends CompositeAppState {
    public static final int WIDTH = 0;
    public static final int HEIGHT = 1;
    static Logger log = LoggerFactory.getLogger(OptionsMenuState.class);
    private Dropdown<String> dropdown = new Dropdown<>();
    private MainMenuState parent;
    private Container optionsWindow;

    public OptionsMenuState(MainMenuState parent) {
        this.parent = parent;
    }

    private void apply() {
        AppSettings settings = ((ANJRpg)getApplication()).getSettings();
        String selection = dropdown.getModel().get(dropdown.getSelectionModel().getSelection());
        List<String> resolution = Arrays.asList(selection.split("x"));
        resolution.replaceAll(String::trim);
        settings.setResolution(Integer.parseInt(resolution.get(WIDTH)), Integer.parseInt(resolution.get(HEIGHT)));
        getApplication().setSettings(settings);


        setEnabled(false);
        parent.setEnabled(false);

        getApplication().restart();

        parent.setEnabled(true);
    }

    @Override
    protected void initialize( Application app ) {
    }

    @Override
    protected void cleanup(Application app) {
        getState(MainMenuState.class).setEnabled(true);
    }

    @Override
    protected void onEnable() {
        optionsWindow = new Container();

        Container menuContainer = optionsWindow.addChild(new Container(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.Even)));
        Label title = menuContainer.addChild(new Label("Options"));
        title.setFontSize(32);
        title.setInsets(new Insets3f(10, 10, 0, 10));

        Container props;
        Container joinPanel = menuContainer.addChild(new Container());
        joinPanel.setInsets(new Insets3f(10, 10, 10, 10));
        props = joinPanel.addChild(new Container(new BorderLayout()));
        props.setBackground(null);

        props.addChild(new Label("Resolution:"), West);
        props.addChild(dropdown, East);


        ActionButton options = menuContainer.addChild(new ActionButton(new CallMethodAction("Apply", this, "apply")));
        options.setInsets(new Insets3f(10, 10, 10, 10));


        parent.getMainWindow().addChild(optionsWindow, East);
        GuiGlobals.getInstance().requestFocus(optionsWindow);
    }

    @Override
    protected void onDisable() {
        optionsWindow.removeFromParent();
    }
}
