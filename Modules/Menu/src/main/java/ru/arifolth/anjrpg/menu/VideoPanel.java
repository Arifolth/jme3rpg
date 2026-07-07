package ru.arifolth.anjrpg.menu;

import com.jme3.app.Application;
import com.jme3.system.AppSettings;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.component.SpringGridLayout;
import ru.arifolth.anjrpg.interfaces.ANJRpgInterface;
import ru.arifolth.anjrpg.interfaces.SoundTypeEnum;
import ru.arifolth.anjrpg.interfaces.ViewDistanceSettings;

import java.util.Arrays;
import java.util.List;

import static com.simsilica.lemur.component.BorderLayout.Position.East;
import static com.simsilica.lemur.component.BorderLayout.Position.West;

public class VideoPanel implements MenuPanel {
    private final Application application;
    private Container container;
    private SystemPanel parentPanel;

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

    public VideoPanel(Application app, SystemPanel parentPanel) {
        this.application = app;
        this.parentPanel = parentPanel;

        container = GameUI.createPanelContainer();
        container.setLayout(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.None));

        Label title = container.addChild(new Label("Video Settings"));
        title.setFontSize(32);
        title.setInsets(new Insets3f(10, 10, 0, 10));

        initialize();
        buildUI();
    }

    private void buildUI() {
        Container props;
        Container panelContainer = new Container();
        panelContainer.setBackground(null);
        Container joinPanel = container.addChild(panelContainer);
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

        joinPanel.addChild(fullscreen);
        joinPanel.addChild(vsync);

        props = joinPanel.addChild(new Container(new BorderLayout()));
        props.setBackground(null);
        props.addChild(new Label("View Distance:"), West);
        props.addChild(viewDistanceDropDown, East);

        props = joinPanel.addChild(new Container(new BorderLayout()));
        props.setBackground(null);
        props.addChild(new ActionButton(new CallMethodAction("Apply", this, "apply")), West);
    }

    private void initialize() {
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
    public Container getContainer() { return container; }

    private void apply() {
        ((ANJRpgInterface) application).getSoundManager().getSoundNode(SoundTypeEnum.MENU).play();

        AppSettings settings = ((ANJRpgInterface) application).getSettings();

        applyRenderer(settings);
        applyResolution(settings);
        applyFrameRate(settings);
        applyBitsPerPixel(settings);
        applySamples(settings);
        applyFullScreen(settings);
        applyVSync(settings);
        applyViewDistance(settings);

        SettingsUtils.saveSettings(settings);
        application.getContext().setSettings(settings);

        // FIX: Explicitly clear the sub-panel BEFORE restarting the context.
        // This detaches the VideoPanel (and its Dropdowns) from the scene graph,
        // preventing Lemur's PopupState from rendering them fully opened in the new context.
        if (parentPanel != null) {
            parentPanel.clearSubPanel();
        }

        InGameMenuState inGameMenu = application.getStateManager().getState(InGameMenuState.class);
        inGameMenu.setEnabled(false);
        application.getContext().restart();
        inGameMenu.setEnabled(true);
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
}