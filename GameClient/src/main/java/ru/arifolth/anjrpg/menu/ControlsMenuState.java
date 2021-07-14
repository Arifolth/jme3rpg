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
import com.jme3.system.AppSettings;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.state.CompositeAppState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.arifolth.anjrpg.ANJRpg;

import static com.simsilica.lemur.component.BorderLayout.Position.East;

public class ControlsMenuState extends CompositeAppState {
    static Logger log = LoggerFactory.getLogger(ControlsMenuState.class);
    public static final int WIDTH = 0;
    public static final int HEIGHT = 1;
    private OptionsMenuState parent;
    private Container audioOptionsWindow;
    private TextField forwardBinding;
    private TextField backwardBinding;
    private TextField leftBinding;
    private TextField rightBinding;
    private TextField jumpBinding;
    private TextField actionBinding;
    private TextField runBinding;

    public ControlsMenuState(OptionsMenuState parent) {
        this.parent = parent;
    }

    private void apply() {
        AppSettings settings = ((ANJRpg)getApplication()).getSettings();

        getApplication().setSettings(settings);

        //Apply options here
        forwardBinding.getText();

        setEnabled(false);
        parent.setEnabled(false);
        parent.getParent().setEnabled(false);

        getApplication().restart();

        parent.getParent().setEnabled(true);
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
        audioOptionsWindow = new Container();

        Container menuContainer = audioOptionsWindow.addChild(new Container(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.Even)));
        Label title = menuContainer.addChild(new Label("Controls"));
        title.setFontSize(24);
        title.setInsets(new Insets3f(10, 10, 0, 10));

        Container props;
        Container joinPanel = menuContainer.addChild(new Container());
        joinPanel.setInsets(new Insets3f(10, 10, 10, 10));
        props = joinPanel.addChild(new Container(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.Last)));
        props.setBackground(null);

        //Options go here
        props.addChild(new Label("Move forward:"));
        forwardBinding = props.addChild(new TextField("W"), 1);

        props.addChild(new Label("Move backwards:"));
        backwardBinding = props.addChild(new TextField("S"), 1);

        props.addChild(new Label("Move left:"));
        leftBinding = props.addChild(new TextField("A"), 1);

        props.addChild(new Label("Move right:"));
        rightBinding = props.addChild(new TextField("D"), 1);

        props.addChild(new Label("Jump:"));
        jumpBinding = props.addChild(new TextField("Space"), 1);

        props.addChild(new Label("Run:"));
        runBinding = props.addChild(new TextField("Shift"), 1);

        props.addChild(new Label("Action:"));
        actionBinding = props.addChild(new TextField("E"), 1);
        /**/
        ActionButton options = menuContainer.addChild(new ActionButton(new CallMethodAction("Apply", this, "apply")));
        options.setInsets(new Insets3f(10, 10, 10, 10));


        parent.getMainWindow().addChild(audioOptionsWindow, East);
        GuiGlobals.getInstance().requestFocus(audioOptionsWindow);
    }

    @Override
    protected void onDisable() {
        audioOptionsWindow.removeFromParent();
    }
}
