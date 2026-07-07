package ru.arifolth.anjrpg.menu;

import com.jme3.app.Application;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.component.DynamicInsetsComponent;
import com.simsilica.lemur.component.SpringGridLayout;
import ru.arifolth.anjrpg.interfaces.ANJRpgInterface;
import ru.arifolth.anjrpg.interfaces.GameLogicCoreInterface;
import ru.arifolth.anjrpg.interfaces.SoundManagerInterface;
import ru.arifolth.anjrpg.interfaces.SoundTypeEnum;

import static com.simsilica.lemur.component.BorderLayout.Position.East;
import static com.simsilica.lemur.component.BorderLayout.Position.West;

public class AudioPanel implements MenuPanel {
    private final Application application;
    private final SoundManagerInterface soundManager;
    private final GameLogicCoreInterface gameLogicCore;
    private Container container;

    // Made optional to support usage in both InGameMenu (SystemPanel) and MainMenu (AudioMenuState)
    private SystemPanel parentPanel;

    private RangedValueModel volumeModel = new DefaultRangedValueModel(0, 1, 0.5);

    public AudioPanel(Application app, SystemPanel parentPanel) {
        this.application = app;
        this.parentPanel = parentPanel;
        this.soundManager = ((ANJRpgInterface) app).getSoundManager();
        this.gameLogicCore = ((ANJRpgInterface) app).getGameLogicCore();

        container = GameUI.createPanelContainer();
        container.setLayout(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.None));

        Label title = container.addChild(new Label("Audio Settings"));
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

        props = joinPanel.addChild(new Container(new BorderLayout()));
        props.setBackground(null);
        props.addChild(new Label("Audio Volume:"), West);

        Slider slider = props.addChild(new Slider(volumeModel), East);
        slider.getDecrementButton().addClickCommands();
        slider.setInsetsComponent(new DynamicInsetsComponent(0.5f, 0.5f, 0.5f, 0.5f));

        props = joinPanel.addChild(new Container(new BorderLayout()));
        props.setBackground(null);
        props.addChild(new ActionButton(new CallMethodAction("Apply", this, "apply")), West);

        // Add Back button only for Main Menu context (when parentPanel is null)
        if (parentPanel == null) {
            props.addChild(new ActionButton(new CallMethodAction("Back", this, "back")), East);
        }
    }

    @Override
    public Container getContainer() {
        return container;
    }

    private void apply() {
        gameLogicCore.getSoundManager().getSoundNode(SoundTypeEnum.MENU).play();

        // Apply actual audio logic
        soundManager.setVolume((float) volumeModel.getValue());
        soundManager.reInitialize(gameLogicCore);
        gameLogicCore.reInitialize();

        if (parentPanel != null) {
            // In-Game Context: Return to SystemPanel root (options list)
            parentPanel.clearSubPanel();
            InGameMenuState inGameMenu = application.getStateManager().getState(InGameMenuState.class);
            if(inGameMenu == null)
                return;
            inGameMenu.setEnabled(false);
            application.getContext().restart();
            inGameMenu.setEnabled(true);
        } else {
            // Main Menu Context: Directly disable OptionsMenuState to return to MainMenu root
            OptionsMenuState optionsMenu = (OptionsMenuState) application.getStateManager().getState(OptionsMenuState.class);
            if(optionsMenu == null)
                return;
            optionsMenu.setEnabled(false);
            application.getContext().restart();
            optionsMenu.setEnabled(true);
        }
    }

    private void back() {
        gameLogicCore.getSoundManager().getSoundNode(SoundTypeEnum.MENU).play();
        if (parentPanel == null) {
            // Main Menu Context: Directly disable OptionsMenuState to return to MainMenu root
            OptionsMenuState optionsMenu = (OptionsMenuState) application.getStateManager().getState(OptionsMenuState.class);
            if (optionsMenu != null) {
                optionsMenu.setEnabled(false);
            }
        }
    }
}