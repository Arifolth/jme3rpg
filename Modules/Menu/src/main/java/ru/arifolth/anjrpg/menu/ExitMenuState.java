package ru.arifolth.anjrpg.menu;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.component.SpringGridLayout;
import ru.arifolth.anjrpg.interfaces.ANJRpgInterface;
import ru.arifolth.anjrpg.interfaces.GameLogicCoreInterface;
import ru.arifolth.anjrpg.interfaces.SoundTypeEnum;

import static com.simsilica.lemur.component.BorderLayout.Position.Center;

public class ExitMenuState extends BaseAppState {
    private Container mainWindow;
    private Container parentWindow;
    private BaseAppState parentState;
    private Node gui;
    private GameLogicCoreInterface gameLogicCore;
    private PressAnyKeyState pressAnyKeyState;
    private String message;

    // 1. For MainMenuState: new ExitMenuState(mainWindow, this)
    // Changed MainMenuState to BaseAppState to fix module visibility
    public ExitMenuState(Container parentWindow, BaseAppState parentState) {
        this.parentWindow = parentWindow;
        this.parentState = parentState;
        this.message = "Exit Game?";
    }

    // 2. For InGameMenuState (SystemPanel): new ExitMenuState(inGameMenu)
    public ExitMenuState(BaseAppState parentState) {
        this.parentState = parentState;
        this.parentWindow = resolveParentWindow(parentState);
        this.message = "Exit Game?";
    }

    // 3. For VideoMenuState: new ExitMenuState(parent.getParent(), "Restart required...")
    public ExitMenuState(BaseAppState parentState, String message) {
        this.parentState = parentState;
        this.message = message;
        this.parentWindow = resolveParentWindow(parentState);
    }

    public BaseAppState getParent() { return parentState; }
    public Container getParentWindow() { return parentWindow; }
    public Container getMainWindow() { return mainWindow; }

    @Override
    protected void initialize(Application app) {
        ANJRpgInterface application = (ANJRpgInterface) app;
        gameLogicCore = application.getGameLogicCore();
        gui = ((SimpleApplication) application).getGuiNode();
        pressAnyKeyState = getStateManager().getState(PressAnyKeyState.class);
        buildUI();
    }

    private void buildUI() {
        mainWindow = new Container(new BorderLayout());
        mainWindow.setBackground(null);

        Container centerPanel = new Container(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.Even));
        centerPanel.setBackground(null);
        mainWindow.addChild(centerPanel, Center);

        Label title = centerPanel.addChild(new Label(message != null ? message : "Exit Game?"));
        title.setFontSize(32);
        title.setInsets(new Insets3f(10, 10, 0, 10));

        ActionButton yes = centerPanel.addChild(new ActionButton(new CallMethodAction("Yes", this, "exitGame")));
        yes.setInsets(new Insets3f(10, 10, 10, 10));

        ActionButton no = centerPanel.addChild(new ActionButton(new CallMethodAction("No", this, "back")));
        no.setInsets(new Insets3f(10, 10, 10, 10));
    }

    @Override
    protected void onEnable() {
        gameLogicCore.getFreeFollowCamera().setEnabled(false);
        GuiGlobals.getInstance().requestCursorEnabled(this);
        gui.attachChild(pressAnyKeyState.getBackgroundPicture());

        if (parentWindow != null) parentWindow.setCullHint(Spatial.CullHint.Always);

        setWindowSize(((ANJRpgInterface)getApplication()).getSettings().getHeight());

        gui.attachChild(mainWindow);
        GuiGlobals.getInstance().requestFocus(mainWindow);
    }

    @Override
    protected void onDisable() {
        gameLogicCore.getFreeFollowCamera().setEnabled(true);
        GuiGlobals.getInstance().releaseCursorEnabled(this);
        gui.detachChild(pressAnyKeyState.getBackgroundPicture());
        mainWindow.removeFromParent();

        if (parentWindow != null) parentWindow.setCullHint(Spatial.CullHint.Inherit);
    }

    public void back() {
        gameLogicCore.getSoundManager().getSoundNode(SoundTypeEnum.MENU).play();
        this.setEnabled(false);
    }

    public void exitGame() {
        gameLogicCore.getSoundManager().getSoundNode(SoundTypeEnum.MENU).play();
        getApplication().stop();
    }

    private void setWindowSize(int height) {
        Vector3f pref = mainWindow.getPreferredSize().clone();
        float standardScale = ((ANJRpgInterface)getApplication()).getCamera().getHeight() / (((ANJRpgInterface)getApplication()).getCamera().getHeight() / 2f);
        pref.multLocal(1.5f * standardScale);

        int width = ((ANJRpgInterface)getApplication()).getSettings().getWidth();
        float x = (width - pref.x) * 0.5f;
        float y = height * 0.5f + pref.y * 0.45f;

        mainWindow.setLocalTranslation(x, y, 0);
        mainWindow.setLocalScale(1.5f * standardScale);
    }

    /**
     * Resolves the parent window to hide.
     * Uses reflection as a fallback to support MainMenuState without requiring a direct module dependency.
     */
    private Container resolveParentWindow(BaseAppState state) {
        if (state instanceof InGameMenuState) return ((InGameMenuState) state).getMainWindow();
        if (state instanceof OptionsMenuState) return ((OptionsMenuState) state).getMainWindow();
        if (state instanceof ExitMenuState) return ((ExitMenuState) state).getMainWindow();

        // Fallback using reflection for MainMenuState or any other state with getMainWindow()
        try {
            java.lang.reflect.Method method = state.getClass().getMethod("getMainWindow");
            return (Container) method.invoke(state);
        } catch (Exception e) {
            return null;
        }
    }

    @Override protected void cleanup(Application app) {}
}