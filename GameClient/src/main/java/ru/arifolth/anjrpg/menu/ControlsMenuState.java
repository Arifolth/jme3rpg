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
import com.jme3.input.KeyInput;
import com.jme3.system.AppSettings;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.state.CompositeAppState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.arifolth.anjrpg.ANJRpg;
import ru.arifolth.anjrpg.BindingConstants;
import ru.arifolth.anjrpg.MovementController;

import static com.simsilica.lemur.component.BorderLayout.Position.East;
import static com.simsilica.lemur.component.BorderLayout.Position.West;
import static ru.arifolth.anjrpg.BindingConstants.*;

public class ControlsMenuState extends CompositeAppState {
    static Logger log = LoggerFactory.getLogger(ControlsMenuState.class);
    private final OptionsMenuState parent;
    private final MovementController movementController;
    private Container audioOptionsWindow;
    private final AppSettings settings;
    private final Dropdown forwardBindingDropDown = new KeyBindingDropDown(UP);
    private final Dropdown backwardBindingDropDown = new KeyBindingDropDown(DOWN);
    private final Dropdown leftBindingDropDown = new KeyBindingDropDown(LEFT);
    private final Dropdown rightBindingDropDown = new KeyBindingDropDown(RIGHT);
    private final Dropdown jumpBindingDropDown = new KeyBindingDropDown(JUMP);
    private final Dropdown runBindingDropDown = new KeyBindingDropDown(RUN);

    public ControlsMenuState(OptionsMenuState parent) {
        this.parent = parent;

        settings = this.parent.getApplication().getContext().getSettings();

        this.movementController = ((ANJRpg) parent.getApplication()).getGameLogicCore().getMovementController();

        this.forwardBindingDropDown.initialize(settings);
        this.backwardBindingDropDown.initialize(settings);
        this.leftBindingDropDown.initialize(settings);
        this.rightBindingDropDown.initialize(settings);
        this.jumpBindingDropDown.initialize(settings);
        this.runBindingDropDown.initialize(settings);
    }

    private void apply() {
        //Apply options here
        try {
            settings.put(UP.name(), MenuUtils.getKey(forwardBindingDropDown.getSelectedValue()));
            settings.put(DOWN.name(), MenuUtils.getKey(backwardBindingDropDown.getSelectedValue()));
            settings.put(LEFT.name(), MenuUtils.getKey(leftBindingDropDown.getSelectedValue()));
            settings.put(RIGHT.name(), MenuUtils.getKey(rightBindingDropDown.getSelectedValue()));
            settings.put(JUMP.name(), MenuUtils.getKey(jumpBindingDropDown.getSelectedValue()));
            settings.put(RUN.name(), MenuUtils.getKey(runBindingDropDown.getSelectedValue()));
        } catch (NoSuchFieldException|IllegalAccessException e) {
            e.printStackTrace();
        }

        movementController.addInputMapping(UP, (Integer) settings.get(UP.name()));
        movementController.addInputMapping(DOWN, (Integer) settings.get(DOWN.name()));
        movementController.addInputMapping(LEFT, (Integer) settings.get(LEFT.name()));
        movementController.addInputMapping(RIGHT, (Integer) settings.get(RIGHT.name()));
        movementController.addInputMapping(JUMP, (Integer) settings.get(JUMP.name()));
        movementController.addInputMapping(RUN, (Integer) settings.get(RUN.name()));

        setEnabled(false);
        parent.setEnabled(false);

        MenuUtils.saveSettings(settings);
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
        props = joinPanel.addChild(new Container(new BorderLayout()));
        props.setBackground(null);
        props.addChild(new Label("Move forward:"), West);
        props.addChild(forwardBindingDropDown, East);

        props = joinPanel.addChild(new Container(new BorderLayout()));
        props.setBackground(null);
        props.addChild(new Label("Move backwards:"), West);
        props.addChild(backwardBindingDropDown, East);

        props = joinPanel.addChild(new Container(new BorderLayout()));
        props.setBackground(null);
        props.addChild(new Label("Move left:"), West);
        props.addChild(leftBindingDropDown, East);

        props = joinPanel.addChild(new Container(new BorderLayout()));
        props.setBackground(null);
        props.addChild(new Label("Move right:"), West);
        props.addChild(rightBindingDropDown, East);

        props = joinPanel.addChild(new Container(new BorderLayout()));
        props.setBackground(null);
        props.addChild(new Label("Jump:"), West);
        props.addChild(jumpBindingDropDown, East);

        props = joinPanel.addChild(new Container(new BorderLayout()));
        props.setBackground(null);
        props.addChild(new Label("Run:"), West);
        props.addChild(runBindingDropDown, East);

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
