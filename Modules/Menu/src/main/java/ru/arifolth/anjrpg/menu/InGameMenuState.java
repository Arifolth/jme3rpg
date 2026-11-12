/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2014 - 2026 Alexander Nilov
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
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.texture.Texture;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.component.QuadBackgroundComponent;
import com.simsilica.lemur.component.SpringGridLayout;
import ru.arifolth.anjrpg.interfaces.ANJRpgInterface;
import ru.arifolth.anjrpg.interfaces.GameLogicCoreInterface;
import ru.arifolth.anjrpg.interfaces.SoundTypeEnum;

import java.util.logging.Level;
import java.util.logging.Logger;

public class InGameMenuState extends BaseAppState {
    private Container mainWindow;
    private Container navLayout;
    private Container detailPanel;
    private MenuPanel currentPanel;
    private final LazyPanels panels = new LazyPanels();
    private Node gui;
    final private static Logger LOGGER = Logger.getLogger(InGameMenuState.class.getName());
    private GameLogicCoreInterface gameLogicCore;
    private ANJRpgInterface application;
    private PressAnyKeyState pressAnyKeyState;
    private Texture bgNavPanelTexture;
    private Texture bgDetailPanelTexture;

    // Stores the name of the last active sub-panel in the System menu
    private String lastSystemSubPanel;

    public InGameMenuState() {
        setEnabled(false);
    }

    @Override
    protected void initialize(Application app) {
        application = (ANJRpgInterface) app;
        gameLogicCore = application.getGameLogicCore();
        gui = ((SimpleApplication) application).getGuiNode();
        pressAnyKeyState = getStateManager().getState(PressAnyKeyState.class);

        // Read and decode the two background images now, while the game loads, not in the middle of the frame
        // that handles ESC. Holding on to them also keeps the asset cache from letting them go.
        bgNavPanelTexture = application.getAssetManager().loadTexture("Interface/rpg_background_panel.png");
        bgDetailPanelTexture = application.getAssetManager().loadTexture("Interface/rpg_background_layout.png");

        // Note: Panels are no longer created here. They are created in onEnable()
        // to ensure fresh UI components and to pass the lastSystemSubPanel state.
    }

    @Override
    protected void onEnable() {
        long started = System.nanoTime();
        gameLogicCore.getFreeFollowCamera().setEnabled(false);
        GuiGlobals.getInstance().requestCursorEnabled(this);
        gui.attachChild(pressAnyKeyState.getBackgroundPicture());

        int screenWidth = application.getSettings().getWidth();
        int screenHeight = application.getSettings().getHeight();
        float standardScale = getStandardScale();
        float totalScale = 1.5f * standardScale;

        float localWidth = screenWidth / totalScale;
        float localHeight = screenHeight / totalScale;

        mainWindow = new Container(new BorderLayout());
        mainWindow.setBackground(null);
        mainWindow.setPreferredSize(new Vector3f(localWidth, localHeight, 0));

        // --- FIX: Recreate panels on every enable to ensure fresh UI components ---
        // Only registered here; each one is built the first time it is shown (see LazyPanels), so
        // pressing ESC no longer builds the System panel's thirteen dropdowns that nobody is looking at.
        panels.clear();
        panels.register("Equipment", () -> new EquipmentPanel(application));
        panels.register("Inventory", () -> new InventoryPanel(application));
        panels.register("Status", () -> new StatusPanel(application));
        panels.register("Journal", () -> new JournalPanel(application));

        // Pass the remembered sub-panel name to SystemPanel so it can restore its state
        panels.register("System", () -> new SystemPanel(application, this, lastSystemSubPanel));
        panels.register("Exit", () -> new ExitPanel(application, this));
        // ---------------------------------------------------------------------------

        // --- WEST SECTION (Navigation) ---
        Container navPanel = GameUI.createPanelContainer();
        navPanel.setLayout(new BorderLayout());

        QuadBackgroundComponent navPanelBackground = new QuadBackgroundComponent(bgNavPanelTexture);
        navPanel.setBackground(navPanelBackground);

        navPanel.setPreferredSize(new Vector3f(localWidth * 0.15f, localHeight, 0));

        navLayout = new Container(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.Even));
        navLayout.setBackground(null);

        navPanel.addChild(navLayout, BorderLayout.Position.Center);
        mainWindow.addChild(navPanel, BorderLayout.Position.West);

        ActionButton backBtn = new ActionButton(new Action() {
            @Override
            public void execute(Button source) {
                playMenuSound();
                InGameMenuState.this.setEnabled(false);
            }

            @Override
            public String getName() {
                return "Back";
            }
        });
        backBtn.setText("Back");
        GameUI.styleNavButton(backBtn);
        navLayout.addChild(backBtn);

        addNavButton("Equipment");
        addNavButton("Inventory");
        addNavButton("Status");
        addNavButton("Journal");
        addNavButton("System");
        addNavButton("Exit");

        // --- CENTER SECTION (Details) ---
        Container detailPanelOuter = GameUI.createPanelContainer();
        detailPanelOuter.setLayout(new BorderLayout());

        QuadBackgroundComponent detailPanelBackground = new QuadBackgroundComponent(bgDetailPanelTexture);
        detailPanelOuter.setBackground(detailPanelBackground);

        detailPanel = GameUI.createPanelContainer();
        detailPanel.setLayout(new BorderLayout());

        float marginX = localWidth * 0.07f;
        float marginY = localHeight * 0.07f;
        detailPanel.setInsets(new Insets3f(marginY, marginX, marginY, marginX));

        detailPanelOuter.addChild(detailPanel, BorderLayout.Position.Center);
        mainWindow.addChild(detailPanelOuter, BorderLayout.Position.Center);

        showPanel("Equipment");

        setWindowSize(screenHeight);

        gui.attachChild(mainWindow);
        GuiGlobals.getInstance().requestFocus(mainWindow);

        LOGGER.log(Level.INFO, String.format("In-game menu built in %.2f ms", (System.nanoTime() - started) / 1_000_000.0));
    }

    private void addNavButton(String name) {
        ActionButton btn = new ActionButton(new Action() {
            @Override
            public void execute(Button source) {
                playMenuSound();
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

    private void playMenuSound() {
        gameLogicCore.getSoundManager().getSoundNode(SoundTypeEnum.MENU).play();
    }

    public void showPanel(String name) {
        long started = System.nanoTime();
        if (currentPanel != null) {
            detailPanel.removeChild(currentPanel.getContainer());
        }
        currentPanel = panels.get(name);
        if (currentPanel != null) {
            detailPanel.addChild(currentPanel.getContainer(), BorderLayout.Position.Center);
        }

        // a panel is built the first time it is shown; say so if that took longer than a frame
        double millis = (System.nanoTime() - started) / 1_000_000.0;
        if (millis > 16.0) {
            LOGGER.log(Level.INFO, String.format("Menu panel '%s' took %.2f ms to show", name, millis));
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

    public float getStandardScale() {
        return application.getCamera().getHeight() / (getApplication().getCamera().getHeight() / 2f);
    }

    private void setWindowSize(int height) {
        float standardScale = getStandardScale();
        float x = 0;
        float y = height;
        mainWindow.setLocalTranslation(x, y, 10);
        mainWindow.setLocalScale(1.5f * standardScale);
    }

    /**
     * Called by SystemPanel to remember which sub-panel (Video, Audio, etc.) was last open.
     */
    public void setLastSystemSubPanel(String name) {
        this.lastSystemSubPanel = name;
    }
}