package ru.arifolth.anjrpg.menu;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.texture.Texture;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.component.QuadBackgroundComponent;
import com.simsilica.lemur.component.SpringGridLayout;
import ru.arifolth.anjrpg.interfaces.ANJRpgInterface;
import ru.arifolth.anjrpg.interfaces.GameLogicCoreInterface;

import java.util.HashMap;
import java.util.Map;

public class InGameMenuState extends BaseAppState {
    private Container mainWindow;
    private Container navLayout;
    private Container detailPanel;
    private MenuPanel currentPanel;
    private final Map<String, MenuPanel> panels = new HashMap<>();
    private Node gui;
    private GameLogicCoreInterface gameLogicCore;
    private ANJRpgInterface application;
    private PressAnyKeyState pressAnyKeyState;

    public InGameMenuState() {
        setEnabled(false);
    }

    @Override
    protected void initialize(Application app) {
        application = (ANJRpgInterface) app;

        panels.put("Equipment", new EquipmentPanel(app));
        panels.put("Inventory", new InventoryPanel(app));
        panels.put("Status", new StatusPanel(app));
        panels.put("Journal", new JournalPanel(app));
        panels.put("Worldmap", new WorldmapPanel(app));
        panels.put("System", new SystemPanel(app));

        gameLogicCore = application.getGameLogicCore();
        gui = ((SimpleApplication) application).getGuiNode();
        pressAnyKeyState = getStateManager().getState(PressAnyKeyState.class);
    }

    @Override
    protected void onEnable() {
        gameLogicCore.getFreeFollowCamera().setEnabled(false);
        GuiGlobals.getInstance().requestCursorEnabled(this);
        gui.attachChild(pressAnyKeyState.getBackgroundPicture());

        int screenWidth = application.getSettings().getWidth();
        int screenHeight = application.getSettings().getHeight();
        float standardScale = getStandardScale();
        float totalScale = 1.5f * standardScale;

        // Calculate local dimensions so that after scaling, they match the screen size
        float localWidth = screenWidth / totalScale;
        float localHeight = screenHeight / totalScale;

        mainWindow = new Container(new BorderLayout());
        mainWindow.setBackground(null);
        mainWindow.setPreferredSize(new Vector3f(localWidth, localHeight, 0));

        // --- WEST SECTION (Navigation) ---
        Container navPanel = GameUI.createPanelContainer();
        navPanel.setLayout(new BorderLayout());

        // Load and apply background for the outer navPanel
        Texture bgNavPanelTexture = application.getAssetManager().loadTexture("Interface/rpg_background_panel.png");
        QuadBackgroundComponent navPanelBackground = new QuadBackgroundComponent(bgNavPanelTexture);
        navPanel.setBackground(navPanelBackground);

        // Set width to exactly 15% of the screen width
        navPanel.setPreferredSize(new Vector3f(localWidth * 0.15f, localHeight, 0));

        navLayout = new Container(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.Even));
        navLayout.setBackground(null);

        navPanel.addChild(navLayout, BorderLayout.Position.Center);
        mainWindow.addChild(navPanel, BorderLayout.Position.West);

        addNavButton("Equipment");
        addNavButton("Inventory");
        addNavButton("Status");
        addNavButton("Journal");
        addNavButton("Worldmap");
        addNavButton("System");

        // --- CENTER SECTION (Details) ---
        // FIX: Create an outer container to hold the background independently of the content margins
        Container detailPanelOuter = GameUI.createPanelContainer();
        detailPanelOuter.setLayout(new BorderLayout());

        // Load and apply background for the central detail panel
        Texture bgDetailPanelTexture = application.getAssetManager().loadTexture("Interface/rpg_background_layout.png");
        QuadBackgroundComponent detailPanelBackground = new QuadBackgroundComponent(bgDetailPanelTexture);
        detailPanelOuter.setBackground(detailPanelBackground);

        // Inner container for the actual content
        detailPanel = GameUI.createPanelContainer();
        detailPanel.setLayout(new BorderLayout());

        // FIX: Add 7% margin from all borders to the content container
        float marginX = localWidth * 0.07f;
        float marginY = localHeight * 0.07f;
        detailPanel.setInsets(new Insets3f(marginY, marginX, marginY, marginX));

        // Add the inner content container to the outer background container
        detailPanelOuter.addChild(detailPanel, BorderLayout.Position.Center);

        // Add the outer container to the main window
        mainWindow.addChild(detailPanelOuter, BorderLayout.Position.Center);

        showPanel("Equipment");

        setWindowSize(screenHeight);

        gui.attachChild(mainWindow);
        GuiGlobals.getInstance().requestFocus(mainWindow);
    }

    private void addNavButton(String name) {
        ActionButton btn = new ActionButton(new Action() {
            @Override
            public void execute(Button source) {
                showPanel(name);
            }

            @Override
            public String getName() {
                return name;
            }
        });

        btn.setText(name);
        GameUI.styleNavButton(btn);
        navLayout.addChild(btn);
    }

    public void showPanel(String name) {
        if (currentPanel != null) {
            detailPanel.removeChild(currentPanel.getContainer());
        }

        currentPanel = panels.get(name);
        if (currentPanel != null) {
            detailPanel.addChild(currentPanel.getContainer(), BorderLayout.Position.Center);
        }
    }

    @Override
    protected void onDisable() {
        gameLogicCore.getFreeFollowCamera().setEnabled(true);
        GuiGlobals.getInstance().releaseCursorEnabled(this);

        currentPanel = null;

        gui.detachChild(pressAnyKeyState.getBackgroundPicture());
        mainWindow.removeFromParent();
    }

    @Override
    protected void cleanup(Application app) {
        panels.clear();
    }

    public Container getMainWindow() {
        return mainWindow;
    }

    public float getStandardScale() {
        return application.getCamera().getHeight() / (getApplication().getCamera().getHeight() / 2f);
    }

    private void setWindowSize(int height) {
        float standardScale = getStandardScale();

        // Pin the menu perfectly to the top-left corner to remove gaps
        float x = 0;
        float y = height;

        mainWindow.setLocalTranslation(x, y, 10); // Z=10 ensures it renders on top of compass/bars
        mainWindow.setLocalScale(1.5f * standardScale);
    }
}