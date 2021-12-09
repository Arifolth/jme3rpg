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

package ru.arifolth.anjrpg.menu;

import com.jme3.app.Application;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.state.CompositeAppState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.simsilica.lemur.component.BorderLayout.Position.*;

public class ExitMenuState extends CompositeAppState {
    private final static Logger LOGGER = LoggerFactory.getLogger(OptionsMenuState.class);
    private MainMenuState parent;
    private Container window;

    public ExitMenuState(MainMenuState parent) {
        this.parent = parent;
    }


    @Override
    protected void initialize( Application app ) {
    }

    @Override
    protected void cleanup(Application app) {
        getState(MainMenuState.class).setEnabled(true);
    }

    public Container getMainWindow() {
        return window;
    }

    @Override
    protected void onEnable() {
        window = new Container(new BorderLayout());

        parent.getMainWindow().clearChildren();

        setWindowSize();

        Container menuContainer = window.addChild(new Container(new BorderLayout()));
        Label title = menuContainer.addChild(new Label("Exit game?"), North);
        title.setFontSize(32);
        title.setInsets(new Insets3f(0, 0, 10, 0));

        Container props = menuContainer.addChild(new Container(new BorderLayout()), South);
        props.setBackground(null);
        props.addChild(new ActionButton(new CallMethodAction("Yes", getApplication(), "stop")), West);
        props.addChild(new ActionButton(new CallMethodAction("No", this, "onDisable")), East);

        parent.getMainWindow().addChild(window);
        GuiGlobals.getInstance().requestFocus(window);
    }

    private void setWindowSize() {
        float standardScale = parent.getStandardScale();
        window.setLocalScale(2.0f / standardScale);
    }


    @Override
    protected void onDisable() {
        window.removeFromParent();
        parent.onEnable();
    }

    public MainMenuState getParent() {
        return parent;
    }
}
