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
import com.simsilica.lemur.component.*;
import com.simsilica.state.CompositeAppState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.arifolth.anjrpg.RolePlayingGame;
import ru.arifolth.game.SoundManager;

import static com.simsilica.lemur.component.BorderLayout.Position.*;

public class AudioMenuState extends CustomCompositeAppState {
    static Logger log = LoggerFactory.getLogger(AudioMenuState.class);
    public static final int WIDTH = 0;
    public static final int HEIGHT = 1;
    private final SoundManager soundManager;
    private RangedValueModel volumeModel = new DefaultRangedValueModel(0, 6, 3);

    public AudioMenuState(OptionsMenuState parent) {
        super(parent);
        this.soundManager = ((RolePlayingGame) this.parent.getApplication()).getSoundManager();
    }

    private void apply() {
        soundManager.setVolume((float) volumeModel.getValue());
        soundManager.reInitialize();
        ((RolePlayingGame) getApplication()).getGameLogicCore().reInitialize();

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
        window = new Container();

        Container menuContainer = window.addChild(new Container(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.Even)));
        Label title = menuContainer.addChild(new Label("Audio"));
        title.setFontSize(24);
        title.setInsets(new Insets3f(10, 10, 0, 10));

        Container props;
        Container joinPanel = menuContainer.addChild(new Container());
        joinPanel.setInsets(new Insets3f(10, 10, 10, 10));
        props = joinPanel.addChild(new Container(new BorderLayout()));
        props.setBackground(null);

        //Options go here
        props.addChild(new Label("Audio Volume:"), West);
        Slider slider = props.addChild(new Slider(volumeModel), East);
        slider.getDecrementButton().addClickCommands();
        slider.setInsetsComponent(new DynamicInsetsComponent(0.5f, 0.5f, 0.5f, 0.5f));

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
}
