/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2014 - 2026 Alexander Nilov
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
import ru.arifolth.anjrpg.interfaces.*;

import java.util.Arrays;
import java.util.List;

import static com.simsilica.lemur.component.BorderLayout.Position.East;
import static com.simsilica.lemur.component.BorderLayout.Position.West;

public class VideoMenuState extends CustomCompositeAppState {
    private final static Logger LOGGER = LoggerFactory.getLogger(VideoMenuState.class);
    public static final int WIDTH = 0;
    public static final int HEIGHT = 1;
    private Dropdown rendererDropDown = new RendererDropDown();
    private Dropdown resolutionsDropDown = new ResolutionsDropDown();
    private Dropdown frameRateDropDown = new FrameRateDropDown();
    private Dropdown bitsPerPixelDropDown = new BitsPerPixelDropDown();
    private Dropdown samplesDropDown = new SamplesDropDown();
    private Checkbox fullscreen = new Checkbox("Fullscreen");
    private Checkbox vsync = new Checkbox("VSync");
    private ViewDistanceDropDown viewDistanceDropDown = new ViewDistanceDropDown();

    private ANJRpgInterface application;
    private GameLogicCoreInterface gameLogicCore;

    public VideoMenuState(OptionsMenuState parent) {
        super(parent);
    }

    public OptionsMenuState getParent() {
        return parent;
    }

    private void apply() {
        gameLogicCore.getSoundManager().getSoundNode(SoundTypeEnum.MENU).play();

        AppSettings settings = application.getSettings();
        applyRenderer(settings);
        applyResolution(settings);
        applyFrameRate(settings);
        applyBitsPerPixel(settings);
        applySamples(settings);
        applyFullScreen(settings);
        applyVSync(settings);
        applyViewDistance(settings);

        getApplication().setSettings(settings);
        SettingsUtils.saveSettings(settings);

        getStateManager().attach(new ExitMenuState(parent.getParent(), "Restart required to apply changes."));

        setEnabled(false);
        parent.setEnabled(false);
    }

    private void applyViewDistance(AppSettings settings) {
        settings.put(ViewDistanceSettings.class.getSimpleName(), viewDistanceDropDown.getSelectedValue());
    }

    private void applySamples(AppSettings settings) {
        settings.setSamples(Integer.parseInt(samplesDropDown.getSelectedValue()));
    }

    private void applyRenderer(AppSettings settings) {
        settings.setRenderer(rendererDropDown.getSelectedValue());
    }

    private void applyVSync(AppSettings settings) {
        settings.setVSync(vsync.isChecked());
    }

    private void applyBitsPerPixel(AppSettings settings) {
        settings.setBitsPerPixel(Integer.parseInt(bitsPerPixelDropDown.getSelectedValue()));
    }

    private void applyFullScreen(AppSettings settings) {
        settings.setFullscreen(fullscreen.isChecked());
    }

    private void applyFrameRate(AppSettings settings) {
        settings.setFrameRate(Integer.parseInt(frameRateDropDown.getSelectedValue()));
    }

    private void applyResolution(AppSettings settings) {
        String selection = resolutionsDropDown.getSelectedValue();
        if(selection != null) {
            List<String> resolution = Arrays.asList(selection.split("x"));
            resolution.replaceAll(String::trim);
            settings.setResolution(Integer.parseInt(resolution.get(WIDTH)), Integer.parseInt(resolution.get(HEIGHT)));
        }
    }

    @Override
    protected void initialize(Application app) {
        application = (ANJRpgInterface) app;
        gameLogicCore = application.getGameLogicCore();

        AppSettings settings = application.getContext().getSettings();
        resolutionsDropDown.initialize(settings);
        rendererDropDown.initialize(settings);
        frameRateDropDown.initialize(settings);
        bitsPerPixelDropDown.initialize(settings);
        samplesDropDown.initialize(settings);
        viewDistanceDropDown.initialize(settings);

        vsync.setChecked(settings.getBoolean("VSync"));
        fullscreen.setChecked(settings.getBoolean("Fullscreen"));
    }

    @Override
    protected void cleanup(Application app) {
        getState(MainMenuState.class).setEnabled(true);
    }

    @Override
    protected void onEnable() {
        window = new Container();

        Container contentContainer = new Container(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.Even));
        contentContainer.setBackground(null);

        Container menuContainer = window.addChild(contentContainer);
        Label title = menuContainer.addChild(new Label("Video"));
        title.setFontSize(24);
        title.setInsets(new Insets3f(10, 10, 0, 10));

        Container props;
        Container container = new Container();
        container.setBackground(null);

        Container joinPanel = menuContainer.addChild(container);
        joinPanel.setInsets(new Insets3f(10, 10, 10, 10));

        props = joinPanel.addChild(new Container(new BorderLayout()));
        props.setBackground(null);
        props.addChild(new Label("Renderer:"), West);
        props.addChild(rendererDropDown, East);

        props = joinPanel.addChild(new Container(new BorderLayout()));
        props.setBackground(null);
        props.addChild(new Label("Resolution:"), West);
        props.addChild(resolutionsDropDown, East);

        props = joinPanel.addChild(new Container(new BorderLayout()));
        props.setBackground(null);
        props.addChild(new Label("Frame Rate:"), West);
        props.addChild(frameRateDropDown, East);

        props = joinPanel.addChild(new Container(new BorderLayout()));
        props.setBackground(null);
        props.addChild(new Label("Bits Per Pixel:"), West);
        props.addChild(bitsPerPixelDropDown, East);

        props = joinPanel.addChild(new Container(new BorderLayout()));
        props.setBackground(null);
        props.addChild(new Label("Anti Aliasing:"), West);
        props.addChild(samplesDropDown, East);

        Checkbox checkbox = joinPanel.addChild(fullscreen);

        checkbox = joinPanel.addChild(vsync);

        props = joinPanel.addChild(new Container(new BorderLayout()));
        props.setBackground(null);
        props.addChild(new Label("View Distance:"), West);
        props.addChild(viewDistanceDropDown, East);

        props = joinPanel.addChild(new Container(new BorderLayout()));
        props.setBackground(null);
        props.addChild(new ActionButton(new CallMethodAction("Apply", this, "apply")), West);
        window.setBackground(null);

        parent.getMainWindow().addChild(window, East);
        GuiGlobals.getInstance().requestFocus(window);
    }

    @Override
    protected void onDisable() {
        window.removeFromParent();
    }
}
