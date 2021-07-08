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
import com.jme3.math.Vector3f;
import com.jme3.system.AppSettings;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.*;
import com.simsilica.lemur.core.VersionedReference;
import com.simsilica.state.CompositeAppState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.arifolth.anjrpg.ANJRpg;

import static com.simsilica.lemur.component.BorderLayout.Position.*;

public class AudioMenuState extends CompositeAppState {
    static Logger log = LoggerFactory.getLogger(AudioMenuState.class);
    public static final int WIDTH = 0;
    public static final int HEIGHT = 1;
    private OptionsMenuState parent;
    private Container audioOptionsWindow;
    private VersionedReference<Double> alpha;

    public AudioMenuState(OptionsMenuState parent) {
        this.parent = parent;
    }

    private void apply() {
        AppSettings settings = ((ANJRpg)getApplication()).getSettings();

        getApplication().setSettings(settings);


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
        Slider slider = props.addChild(new Slider(new DefaultRangedValueModel(1, 10, 5)), East);
        alpha = slider.getModel().createReference();

        slider.setInsetsComponent(new DynamicInsetsComponent(0.5f, 0.5f, 0.5f, 0.5f));

        //
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
