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
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.state.CompositeAppState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.simsilica.lemur.component.BorderLayout.Position.East;

public class OptionsMenuState extends CompositeAppState {
    private final static Logger LOGGER = LoggerFactory.getLogger(OptionsMenuState.class);
    private MainMenuState parent;
    private Container optionsWindow;

    public OptionsMenuState(MainMenuState parent) {
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
        return optionsWindow;
    }

    private void video() {
        getStateManager().attach(new VideoMenuState(this));
    }

    private void audio() {
        getStateManager().attach(new AudioMenuState(this));
    }

    private void controls() {
        getStateManager().attach(new ControlsMenuState(this));
    }

    @Override
    protected void onEnable() {
        optionsWindow = new Container(new BorderLayout());

        Container menuContainer = optionsWindow.addChild(new Container(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.Even)));
        Label title = menuContainer.addChild(new Label("Options"));
        title.setFontSize(32);
        title.setInsets(new Insets3f(10, 10, 0, 10));

        ActionButton video = menuContainer.addChild(new ActionButton(new CallMethodAction("Video", this, "video")));
        video.setInsets(new Insets3f(10, 10, 10, 10));

        ActionButton audio = menuContainer.addChild(new ActionButton(new CallMethodAction("Audio", this, "audio")));
        audio.setInsets(new Insets3f(10, 10, 10, 10));

        ActionButton controls = menuContainer.addChild(new ActionButton(new CallMethodAction("Controls", this, "controls")));
        controls.setInsets(new Insets3f(10, 10, 10, 10));

        parent.getMainWindow().addChild(optionsWindow, East);
        GuiGlobals.getInstance().requestFocus(optionsWindow);
    }

    @Override
    protected void onDisable() {
        optionsWindow.removeFromParent();
    }

    public MainMenuState getParent() {
        return parent;
    }
}
