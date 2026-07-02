package ru.arifolth.anjrpg.menu;

import com.jme3.app.Application;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.SpringGridLayout;
import ru.arifolth.anjrpg.interfaces.ANJRpgInterface;
import ru.arifolth.anjrpg.interfaces.SoundTypeEnum;

public class SystemPanel implements MenuPanel {
    private Container container;
    private Application app;

    public SystemPanel(Application app) {
        this.app = app;

        // FIX: Changed FillMode.Even to FillMode.None for the X-axis.
        // This prevents the buttons from stretching to the full width of the panel.
        container = new Container(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.None));
        container.setBackground(null);

        Label title = container.addChild(new Label("System"));
        title.setFontSize(32);
        title.setInsets(new Insets3f(10, 10, 0, 10));

        ActionButton resume = container.addChild(new ActionButton(new CallMethodAction("Resume Game", this, "resumeGame")));
        resume.setInsets(new Insets3f(10, 10, 10, 10)); // Standard insets matching MainMenuState

        ActionButton restart = container.addChild(new ActionButton(new CallMethodAction("Restart Game", this, "restart")));
        restart.setInsets(new Insets3f(10, 10, 10, 10));

        ActionButton options = container.addChild(new ActionButton(new CallMethodAction("Options", this, "options")));
        options.setInsets(new Insets3f(10, 10, 10, 10));

        ActionButton exit = container.addChild(new ActionButton(new CallMethodAction("Exit Game", this, "exitGame")));
        exit.setInsets(new Insets3f(10, 10, 10, 10));
    }

    @Override
    public Container getContainer() { return container; }

    public void resumeGame() {
        ((ANJRpgInterface) app).getGameLogicCore().getSoundManager().getSoundNode(SoundTypeEnum.MENU).play();
        InGameMenuState inGameMenu = app.getStateManager().getState(InGameMenuState.class);
        if (inGameMenu != null) inGameMenu.setEnabled(false);
    }

    public void restart() {
        ((ANJRpgInterface) app).getGameLogicCore().getSoundManager().getSoundNode(SoundTypeEnum.MENU).play();
    }

    public void options() {
        ((ANJRpgInterface) app).getGameLogicCore().getSoundManager().getSoundNode(SoundTypeEnum.MENU).play();
        OptionsMenuState optionsState = app.getStateManager().getState(OptionsMenuState.class);
        if (optionsState == null) {
            InGameMenuState inGameMenu = app.getStateManager().getState(InGameMenuState.class);
            optionsState = new OptionsMenuState(inGameMenu);
            app.getStateManager().attach(optionsState);
        }
        optionsState.setEnabled(true);
    }

    public void exitGame() {
        ((ANJRpgInterface) app).getGameLogicCore().getSoundManager().getSoundNode(SoundTypeEnum.MENU).play();
        ExitMenuState exitState = app.getStateManager().getState(ExitMenuState.class);
        if (exitState == null) {
            InGameMenuState inGameMenu = app.getStateManager().getState(InGameMenuState.class);
            exitState = new ExitMenuState(inGameMenu);
            app.getStateManager().attach(exitState);
        }
        exitState.setEnabled(true);
    }
}