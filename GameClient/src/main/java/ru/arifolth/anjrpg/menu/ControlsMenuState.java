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
import ru.arifolth.anjrpg.MovementController;

import static com.simsilica.lemur.component.BorderLayout.Position.East;
import static com.simsilica.lemur.component.BorderLayout.Position.West;
import static ru.arifolth.anjrpg.BindingConstants.*;

public class ControlsMenuState extends CompositeAppState {
    static Logger log = LoggerFactory.getLogger(ControlsMenuState.class);
    public static final int WIDTH = 0;
    public static final int HEIGHT = 1;
    private OptionsMenuState parent;
    private MovementController movementController;
    private Container audioOptionsWindow;
    private Dropdown forwardBindingDropDown = new KeyBindingDropDown() {
        @Override
        protected void setCurrentValue() {
            try {
                chosenElement.setText(MenuUtils.getResolvedKey("W"));
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
    };
    private Dropdown backwardBindingDropDown = new KeyBindingDropDown() {
        @Override
        protected void setCurrentValue() {
            try {
                chosenElement.setText(MenuUtils.getResolvedKey("S"));
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
    };
    private Dropdown leftBindingDropDown = new KeyBindingDropDown() {
        @Override
        protected void setCurrentValue() {
            try {
                chosenElement.setText(MenuUtils.getResolvedKey("A"));
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
    };
    private Dropdown rightBindingDropDown = new KeyBindingDropDown() {
        @Override
        protected void setCurrentValue() {
            try {
                chosenElement.setText(MenuUtils.getResolvedKey("D"));
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
    };
    private Dropdown jumpBindingDropDown = new KeyBindingDropDown() {
        @Override
        protected void setCurrentValue() {
            try {
                chosenElement.setText(MenuUtils.getResolvedKey("SPACE"));
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
    };
    private Dropdown runBindingDropDown = new KeyBindingDropDown() {
        @Override
        protected void setCurrentValue() {
            try {
                chosenElement.setText(MenuUtils.getResolvedKey("LSHIFT"));
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
    };

    public ControlsMenuState(OptionsMenuState parent) {
        this.parent = parent;

        AppSettings settings = this.parent.getApplication().getContext().getSettings();

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
            movementController.addInputMapping(UP, MenuUtils.getKey(forwardBindingDropDown.getSelectedValue()));
            movementController.addInputMapping(DOWN, MenuUtils.getKey(backwardBindingDropDown.getSelectedValue()));
            movementController.addInputMapping(LEFT, MenuUtils.getKey(leftBindingDropDown.getSelectedValue()));
            movementController.addInputMapping(RIGHT, MenuUtils.getKey(rightBindingDropDown.getSelectedValue()));
            movementController.addInputMapping(JUMP, MenuUtils.getKey(jumpBindingDropDown.getSelectedValue()));
            movementController.addInputMapping(RUN, MenuUtils.getKey(runBindingDropDown.getSelectedValue()));
        } catch (NoSuchFieldException|IllegalAccessException e) {
            e.printStackTrace();
        }

        setEnabled(false);
        parent.setEnabled(false);
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
