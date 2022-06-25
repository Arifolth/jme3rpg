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
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.component.SpringGridLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.arifolth.anjrpg.ANJRpgInterface;
import ru.arifolth.game.BindingConstants;
import ru.arifolth.game.GameLogicCoreInterface;
import ru.arifolth.game.MovementControllerInterface;

import static com.simsilica.lemur.component.BorderLayout.Position.East;
import static com.simsilica.lemur.component.BorderLayout.Position.West;

public class ControlsMenuState extends CustomCompositeAppState {
    private final static Logger LOGGER = LoggerFactory.getLogger(ControlsMenuState.class);
    private MovementControllerInterface movementController;
    private AppSettings settings;
    private final Dropdown forwardBindingDropDown = new KeyBindingDropDown(BindingConstants.UP);
    private final Dropdown backwardBindingDropDown = new KeyBindingDropDown(BindingConstants.DOWN);
    private final Dropdown leftBindingDropDown = new KeyBindingDropDown(BindingConstants.LEFT);
    private final Dropdown rightBindingDropDown = new KeyBindingDropDown(BindingConstants.RIGHT);
    private final Dropdown jumpBindingDropDown = new KeyBindingDropDown(BindingConstants.JUMP);
    private final Dropdown runBindingDropDown = new KeyBindingDropDown(BindingConstants.RUN);
    private ANJRpgInterface application;
    private GameLogicCoreInterface gameLogicCore;

    public ControlsMenuState(OptionsMenuState parent) {
        super(parent);
    }

    public OptionsMenuState getParent() {
        return parent;
    }

    private void apply() {
        gameLogicCore.getSoundManager().getMenuNode().play();

        //Apply options here
        try {
            settings.put(BindingConstants.UP.name(), MenuUtils.getKey(forwardBindingDropDown.getSelectedValue()));
            settings.put(BindingConstants.DOWN.name(), MenuUtils.getKey(backwardBindingDropDown.getSelectedValue()));
            settings.put(BindingConstants.LEFT.name(), MenuUtils.getKey(leftBindingDropDown.getSelectedValue()));
            settings.put(BindingConstants.RIGHT.name(), MenuUtils.getKey(rightBindingDropDown.getSelectedValue()));
            settings.put(BindingConstants.JUMP.name(), MenuUtils.getKey(jumpBindingDropDown.getSelectedValue()));
            settings.put(BindingConstants.RUN.name(), MenuUtils.getKey(runBindingDropDown.getSelectedValue()));
        } catch (NoSuchFieldException|IllegalAccessException e) {
            e.printStackTrace();
        }

        movementController.addInputMapping(BindingConstants.UP, (Integer) settings.get(BindingConstants.UP.name()));
        movementController.addInputMapping(BindingConstants.DOWN, (Integer) settings.get(BindingConstants.DOWN.name()));
        movementController.addInputMapping(BindingConstants.LEFT, (Integer) settings.get(BindingConstants.LEFT.name()));
        movementController.addInputMapping(BindingConstants.RIGHT, (Integer) settings.get(BindingConstants.RIGHT.name()));
        movementController.addInputMapping(BindingConstants.JUMP, (Integer) settings.get(BindingConstants.JUMP.name()));
        movementController.addInputMapping(BindingConstants.RUN, (Integer) settings.get(BindingConstants.RUN.name()));

        setEnabled(false);
        parent.setEnabled(false);

        MenuUtils.saveSettings(settings);
    }

    @Override
    protected void initialize(Application app) {
        application = (ANJRpgInterface) app;
        gameLogicCore = application.getGameLogicCore();

        settings = application.getContext().getSettings();

        this.movementController = gameLogicCore.getMovementController();

        this.forwardBindingDropDown.initialize(settings);
        this.backwardBindingDropDown.initialize(settings);
        this.leftBindingDropDown.initialize(settings);
        this.rightBindingDropDown.initialize(settings);
        this.jumpBindingDropDown.initialize(settings);
        this.runBindingDropDown.initialize(settings);
    }

    @Override
    protected void cleanup(Application app) {
        getState(MainMenuState.class).setEnabled(true);
    }

    @Override
    protected void onEnable() {
        window = new Container();

        Container menuContainer = window.addChild(new Container(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.Even)));
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

        props = joinPanel.addChild(new Container(new BorderLayout()));
        props.setBackground(null);
        props.addChild(new ActionButton(new CallMethodAction("Apply", this, "apply")), West);


        parent.getMainWindow().addChild(window, East);
        GuiGlobals.getInstance().requestFocus(window);
    }

    @Override
    protected void onDisable() {
        gameLogicCore.getSoundManager().getMenuNode().play();

        window.removeFromParent();
    }
}
