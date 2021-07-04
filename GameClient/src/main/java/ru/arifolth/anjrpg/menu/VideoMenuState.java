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
import com.simsilica.state.CompositeAppState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.arifolth.anjrpg.ANJRpg;

import java.util.Arrays;
import java.util.List;

import static com.simsilica.lemur.component.BorderLayout.Position.East;
import static com.simsilica.lemur.component.BorderLayout.Position.West;

public class VideoMenuState extends CompositeAppState {
    static Logger log = LoggerFactory.getLogger(VideoMenuState.class);
    public static final int WIDTH = 0;
    public static final int HEIGHT = 1;
    private Dropdown<String> dropdown = new ResolutionsDropDown();
    private OptionsMenuState parent;
    private Container videoOptionsWindow;

    public VideoMenuState(OptionsMenuState parent) {
        this.parent = parent;
    }

    private void apply() {
        AppSettings settings = ((ANJRpg)getApplication()).getSettings();
        Integer selectionItem = dropdown.getSelectionModel().getSelection();
        if(null == selectionItem)
            selectionItem = dropdown.getDefaultSelection();
        String selection = dropdown.getModel().get(selectionItem);
        List<String> resolution = Arrays.asList(selection.split("x"));
        resolution.replaceAll(String::trim);
        settings.setResolution(Integer.parseInt(resolution.get(WIDTH)), Integer.parseInt(resolution.get(HEIGHT)));
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
        videoOptionsWindow = new Container();

        Container menuContainer = videoOptionsWindow.addChild(new Container(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.Even)));
        Label title = menuContainer.addChild(new Label("Video"));
        title.setFontSize(24);
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

        parent.getMainWindow().addChild(videoOptionsWindow, East);
        GuiGlobals.getInstance().requestFocus(videoOptionsWindow);
    }

    @Override
    protected void onDisable() {
        videoOptionsWindow.removeFromParent();
    }
}
