package ru.arifolth.anjrpg.menu;

import com.jme3.app.Application;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.GuiGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.arifolth.anjrpg.interfaces.ANJRpgInterface;
import ru.arifolth.anjrpg.interfaces.GameLogicCoreInterface;
import ru.arifolth.anjrpg.interfaces.SoundTypeEnum;

import static com.simsilica.lemur.component.BorderLayout.Position.East;

public class GamePlayMenuState extends CustomCompositeAppState {
    private final static Logger LOGGER = LoggerFactory.getLogger(GamePlayMenuState.class);
    private ANJRpgInterface application;
    private GameLogicCoreInterface gameLogicCore;
    private GameplayPanel gameplayPanel;

    public GamePlayMenuState(OptionsMenuState parent) {
        super(parent);
    }

    public OptionsMenuState getParent() {
        return parent;
    }

    @Override
    protected void initialize(Application app) {
        application = (ANJRpgInterface) app;
        gameLogicCore = application.getGameLogicCore();

        // Instantiate the panel. Pass null for SystemPanel since this is used in the MainMenu.
        gameplayPanel = new GameplayPanel(application, null);
    }

    @Override
    protected void cleanup(Application app) {
        getState(MainMenuState.class).setEnabled(true);
    }

    @Override
    protected void onEnable() {
        window = new Container();
        parent.getMainWindow().clearChildren();

        // Reuse the GameplayPanel's container directly, eliminating layout duplication
        Container contentContainer = gameplayPanel.getContainer();
        contentContainer.setBackground(null);

        window.addChild(contentContainer);
        window.setBackground(null);

        parent.getMainWindow().addChild(window, East);
        GuiGlobals.getInstance().requestFocus(window);
    }

    @Override
    protected void onDisable() {
        gameLogicCore.getSoundManager().getSoundNode(SoundTypeEnum.MENU).play();
        window.removeFromParent();

        // Directly disable the OptionsMenuState to return to the Main Menu root.
        // This avoids the broken chain reaction of calling setEnabled(false) on itself.
        if (parent != null) {
            parent.setEnabled(false);
        }
    }
}