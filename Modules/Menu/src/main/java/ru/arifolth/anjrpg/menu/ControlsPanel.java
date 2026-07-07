package ru.arifolth.anjrpg.menu;

import com.jme3.app.Application;
import com.jme3.system.AppSettings;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.component.SpringGridLayout;
import ru.arifolth.anjrpg.interfaces.ANJRpgInterface;
import ru.arifolth.anjrpg.interfaces.BindingConstants;
import ru.arifolth.anjrpg.interfaces.MovementControllerInterface;
import ru.arifolth.anjrpg.interfaces.SoundTypeEnum;

import static com.simsilica.lemur.component.BorderLayout.Position.East;
import static com.simsilica.lemur.component.BorderLayout.Position.West;

public class ControlsPanel implements MenuPanel {
    private final Application application;
    private Container container;
    private SystemPanel parentPanel;

    private final MovementControllerInterface movementController;
    private final AppSettings settings;

    private final Dropdown targetLockBindingDropDown = new KeyBindingDropDown(BindingConstants.LOCK);
    private final Dropdown forwardBindingDropDown = new KeyBindingDropDown(BindingConstants.UP);
    private final Dropdown backwardBindingDropDown = new KeyBindingDropDown(BindingConstants.DOWN);
    private final Dropdown leftBindingDropDown = new KeyBindingDropDown(BindingConstants.LEFT);
    private final Dropdown rightBindingDropDown = new KeyBindingDropDown(BindingConstants.RIGHT);
    private final Dropdown jumpBindingDropDown = new KeyBindingDropDown(BindingConstants.JUMP);
    private final Dropdown runBindingDropDown = new KeyBindingDropDown(BindingConstants.RUN);

    public ControlsPanel(Application app, SystemPanel parentPanel) {
        this.application = app;
        this.parentPanel = parentPanel;

        this.movementController = ((ANJRpgInterface) app).getGameLogicCore().getMovementController();
        this.settings = application.getContext().getSettings();

        // Initialize dropdowns with current settings
        targetLockBindingDropDown.initialize(settings);
        forwardBindingDropDown.initialize(settings);
        backwardBindingDropDown.initialize(settings);
        leftBindingDropDown.initialize(settings);
        rightBindingDropDown.initialize(settings);
        jumpBindingDropDown.initialize(settings);
        runBindingDropDown.initialize(settings);

        container = GameUI.createPanelContainer();
        container.setLayout(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.None));

        Label title = container.addChild(new Label("Controls"));
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
        props.addChild(new Label("Lock on target:"), West);
        props.addChild(targetLockBindingDropDown, East);

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
        ((ANJRpgInterface) application).getSoundManager().getSoundNode(SoundTypeEnum.MENU).play();

        // Apply options
        try {
            settings.put(BindingConstants.UP.name(), MenuUtils.getKey(forwardBindingDropDown.getSelectedValue()));
            settings.put(BindingConstants.DOWN.name(), MenuUtils.getKey(backwardBindingDropDown.getSelectedValue()));
            settings.put(BindingConstants.LEFT.name(), MenuUtils.getKey(leftBindingDropDown.getSelectedValue()));
            settings.put(BindingConstants.RIGHT.name(), MenuUtils.getKey(rightBindingDropDown.getSelectedValue()));
            settings.put(BindingConstants.JUMP.name(), MenuUtils.getKey(jumpBindingDropDown.getSelectedValue()));
            settings.put(BindingConstants.RUN.name(), MenuUtils.getKey(runBindingDropDown.getSelectedValue()));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        movementController.addDefaultInputMapping(BindingConstants.UP);
        movementController.addDefaultInputMapping(BindingConstants.DOWN);
        movementController.addDefaultInputMapping(BindingConstants.LEFT);
        movementController.addDefaultInputMapping(BindingConstants.RIGHT);
        movementController.addDefaultInputMapping(BindingConstants.JUMP);
        movementController.addDefaultInputMapping(BindingConstants.RUN);

        SettingsUtils.saveSettings(settings);

        if (parentPanel != null) {
            // In-Game Context
            parentPanel.setLastSubPanel("Controls");
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
        ((ANJRpgInterface) application).getSoundManager().getSoundNode(SoundTypeEnum.MENU).play();
        if (parentPanel == null) {
            // Main Menu Context: Directly disable OptionsMenuState to return to MainMenu root
            OptionsMenuState optionsMenu = (OptionsMenuState) application.getStateManager().getState(OptionsMenuState.class);
            if (optionsMenu != null) optionsMenu.setEnabled(false);
        }
    }
}