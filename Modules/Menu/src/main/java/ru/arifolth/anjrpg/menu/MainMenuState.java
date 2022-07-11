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
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.component.IconComponent;
import com.simsilica.lemur.component.SpringGridLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.arifolth.anjrpg.ANJRpgInterface;
import ru.arifolth.game.GameLogicCoreInterface;
import ru.arifolth.game.InitializationDelegateInterface;

import static com.simsilica.lemur.component.BorderLayout.Position.West;

public class MainMenuState extends BaseAppState {
    private final static Logger LOGGER = LoggerFactory.getLogger(MainMenuState.class);

    private Container mainWindow;
    private Container menuContainer;
    private ANJRpgInterface application;
    private GameLogicCoreInterface gameLogicCore;

    @Override
    protected void initialize(Application app) {
        application = (ANJRpgInterface) app;
        gameLogicCore = application.getGameLogicCore();
    }

    public MainMenuState() {
    }

    public float getStandardScale() {
        return application.getCamera().getHeight() / (getApplication().getCamera().getHeight() / 2f);
    }

    private void restart() {
        gameLogicCore.getSoundManager().getMenuNode().play();

        InitializationDelegateInterface initializationDelegate = gameLogicCore.getInitializationDelegate();

        if(!application.getGameLogicCore().getPlayerCharacter().isDead()) {
            initializationDelegate.detachNPCs();
            gameLogicCore.getPlayerCharacter().removeCharacter();
        }

        initializationDelegate.initializePlayer(false);

        if(!gameLogicCore.getPlayerCharacter().isDead()) {
            initializationDelegate.initializeNPCs(true);
        }

        setEnabled(false);
    }

    private void exitGame() {
        gameLogicCore.getSoundManager().getMenuNode().play();

        getStateManager().attach(new ExitMenuState(this));
    }

    private void resumeGame() {
        gameLogicCore.getSoundManager().getMenuNode().play();

        setEnabled(false);
    }

    private void startNewGame() {
        gameLogicCore.getSoundManager().getMenuNode().play();

        ((ANJRpgInterface) getApplication()).setUpGUI();
        setEnabled(false);
    }

    private void options() {
        gameLogicCore.getSoundManager().getMenuNode().play();

        getStateManager().attach(new OptionsMenuState(this));
    }

    public Container getMainWindow() {
        return mainWindow;
    }

    @Override
    protected void cleanup( Application app ) {
    }

    public Container getMenuContainer() {
        return menuContainer;
    }

    @Override
    protected void onEnable() {
        ANJRpgInterface application = (ANJRpgInterface) getApplication();

        int width = application.getSettings().getWidth();
        int height = application.getSettings().getHeight();

        mainWindow = new Container(new BorderLayout());

        menuContainer = mainWindow.addChild(new Container(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.Even)), West);
        Label title = menuContainer.addChild(new Label("ANJRpg"));
        title.setFontSize(32);
        title.setInsets(new Insets3f(10, 10, 0, 10));
        Label version = menuContainer.addChild(new Label("Version: " + ((ANJRpgInterface) getApplication()).getVersion()));
        version.setFontSize(12);
        version.setInsets(new Insets3f(10, 10, 0, 10));

        switch(((ANJRpgInterface)getApplication()).getInitStatus()) {
            case RUNNING: {
                mainWindow.setBackground(null);
                ActionButton restart = menuContainer.addChild(new ActionButton(new CallMethodAction("Restart Game", this, "restart")));
                restart.setInsets(new Insets3f(10, 10, 10, 10));

                if(!application.getGameLogicCore().getPlayerCharacter().isDead()) {
                    ActionButton resume = menuContainer.addChild(new ActionButton(new CallMethodAction("Resume Game", this, "resumeGame")));
                    resume.setInsets(new Insets3f(10, 10, 10, 10));
                }
                break;
            }
            default: {
                IconComponent background = new IconComponent("Interface/loading_screen.jpg", 0.32f, 0, 0f, 0f, false);
                background.setOverlay(true);
                background.setIconSize(new Vector2f(width, height));
                mainWindow.setBackground(background);

                ActionButton start = menuContainer.addChild(new ActionButton(new CallMethodAction("Start Game", this, "startNewGame")));
                start.setInsets(new Insets3f(10, 10, 10, 10));
            }

        }

        ActionButton options = menuContainer.addChild(new ActionButton(new CallMethodAction("Options", this, "options")));
        options.setInsets(new Insets3f(10, 10, 10, 10));


        ActionButton exit = menuContainer.addChild(new ActionButton(new CallMethodAction("Exit Game", this, "exitGame")));
        exit.setInsets(new Insets3f(10, 10, 10, 10));


        setWindowSize(height);


        Node gui = ((SimpleApplication) application).getGuiNode();
        gui.attachChild(mainWindow);
        GuiGlobals.getInstance().requestFocus(mainWindow);
    }

    private void setWindowSize(int height) {
        Vector3f pref = mainWindow.getPreferredSize().clone();

        float standardScale = getStandardScale();
        pref.multLocal(1.5f * standardScale);

        // With a slight bias toward the top
        float y = height * 0.5f + pref.y * 0.45f;

        mainWindow.setLocalTranslation(100 * standardScale, y, 0);
        mainWindow.setLocalScale(1.5f * standardScale);
    }

    @Override
    protected void onDisable() {
        mainWindow.removeFromParent();
    }
}
