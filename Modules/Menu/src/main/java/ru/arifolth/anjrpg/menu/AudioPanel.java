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
    private Container container;
    private SystemPanel parentPanel;

    private final SoundManagerInterface soundManager;
    private final GameLogicCoreInterface gameLogicCore;
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
    }

    @Override
    public Container getContainer() {
        return container;
    }

    private void apply() {
        gameLogicCore.getSoundManager().getSoundNode(SoundTypeEnum.MENU).play();

        // Apply audio settings logic from AudioMenuState
        soundManager.setVolume((float) volumeModel.getValue());
        soundManager.reInitialize(gameLogicCore);
        gameLogicCore.reInitialize();

        // Replicate VideoPanel Apply behavior to prevent UI corruption
        if (parentPanel != null) {
            parentPanel.clearSubPanel();
        }
        application.getContext().restart();
    }
}