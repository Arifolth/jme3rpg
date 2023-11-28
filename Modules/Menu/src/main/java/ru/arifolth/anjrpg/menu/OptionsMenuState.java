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
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.state.CompositeAppState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.arifolth.anjrpg.interfaces.ANJRpgInterface;
import ru.arifolth.anjrpg.interfaces.GameLogicCoreInterface;
import ru.arifolth.anjrpg.interfaces.SoundTypeEnum;

import static com.simsilica.lemur.component.BorderLayout.Position.East;

public class OptionsMenuState extends CompositeAppState {
    private final static Logger LOGGER = LoggerFactory.getLogger(OptionsMenuState.class);
    private MainMenuState parent;
    private Container optionsWindow;
    private ANJRpgInterface application;
    private GameLogicCoreInterface gameLogicCore;

    public OptionsMenuState(MainMenuState parent) {
        this.parent = parent;
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

    public Container getMainWindow() {
        return optionsWindow;
    }

    private void video() {
        gameLogicCore.getSoundManager().getSoundNode(SoundTypeEnum.MENU).play();

        getStateManager().attach(new VideoMenuState(this));
    }

    private void audio() {
        gameLogicCore.getSoundManager().getSoundNode(SoundTypeEnum.MENU).play();

        getStateManager().attach(new AudioMenuState(this));
    }

    private void controls() {
        gameLogicCore.getSoundManager().getSoundNode(SoundTypeEnum.MENU).play();

        getStateManager().attach(new ControlsMenuState(this));
    }

    private void gameplay() {
        gameLogicCore.getSoundManager().getSoundNode(SoundTypeEnum.MENU).play();

        getStateManager().attach(new GamePlayMenuState(this));
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

        ActionButton gameplay = menuContainer.addChild(new ActionButton(new CallMethodAction("Gameplay", this, "gameplay")));
        gameplay.setInsets(new Insets3f(10, 10, 10, 10));

        parent.getMainWindow().addChild(optionsWindow, East);
        GuiGlobals.getInstance().requestFocus(optionsWindow);
    }

    @Override
    protected void onDisable() {
        gameLogicCore.getSoundManager().getSoundNode(SoundTypeEnum.MENU).play();

        optionsWindow.removeFromParent();
    }

    public MainMenuState getParent() {
        return parent;
    }
}
