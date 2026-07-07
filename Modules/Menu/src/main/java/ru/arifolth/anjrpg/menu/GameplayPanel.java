package ru.arifolth.anjrpg.menu;

import com.jme3.app.Application;
import com.jme3.system.AppSettings;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.component.SpringGridLayout;
import ru.arifolth.anjrpg.interfaces.ANJRpgInterface;
import ru.arifolth.anjrpg.interfaces.Constants;
import ru.arifolth.anjrpg.interfaces.SoundTypeEnum;

import static com.simsilica.lemur.component.BorderLayout.Position.East;
import static com.simsilica.lemur.component.BorderLayout.Position.West;

public class GameplayPanel implements MenuPanel {
    private final Application application;
    private Container container;
    private SystemPanel parentPanel;

    private Checkbox debugCheckbox = new Checkbox(Constants.DEBUG);

    public GameplayPanel(Application app, SystemPanel parentPanel) {
        this.application = app;
        this.parentPanel = parentPanel;

        // Initialize checkbox with current settings
        AppSettings settings = application.getContext().getSettings();
        debugCheckbox.setChecked(settings.getBoolean(Constants.DEBUG));

        container = GameUI.createPanelContainer();
        container.setLayout(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.None));

        Label title = container.addChild(new Label("Gameplay"));
        title.setFontSize(32);
        title.setInsets(new Insets3f(10, 10, 0, 10));

        buildUI();
    }

    private void buildUI() {
        Container props;
        Container panelContainer = new Container();
        panelContainer.setBackground(null);
        Container joinPanel = container.addChild(panelContainer);
        joinPanel.setInsets(new Insets3f(10, 10, 10, 10));

        joinPanel.addChild(debugCheckbox);
        debugCheckbox.setInsets(new Insets3f(10, 20, 10, 20));

        props = joinPanel.addChild(new Container(new BorderLayout()));
        props.setBackground(null);
        props.addChild(new ActionButton(new CallMethodAction("Apply", this, "apply")), West);
    }

    @Override
    public Container getContainer() {
        return container;
    }

    private void apply() {
        ((ANJRpgInterface) application).getSoundManager().getSoundNode(SoundTypeEnum.MENU).play();

        AppSettings settings = application.getContext().getSettings();
        applySettings(settings);

        SettingsUtils.saveSettings(settings);
        application.getContext().setSettings(settings);

        // Replicate VideoPanel Apply behavior to prevent UI corruption
        if (parentPanel != null) {
            parentPanel.clearSubPanel();
        }
        InGameMenuState inGameMenu = application.getStateManager().getState(InGameMenuState.class);
        inGameMenu.setEnabled(false);
        application.getContext().restart();
        inGameMenu.setEnabled(true);
    }

    private void applySettings(AppSettings settings) {
        settings.putBoolean(Constants.DEBUG, debugCheckbox.isChecked());
    }
}