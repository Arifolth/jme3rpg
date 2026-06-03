/**
 * ANJRpg - an open source Role Playing Game written in Java.
 * Copyright (C) 2014 - 2026 Alexander Nilov
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package ru.arifolth.anjrpg.menu;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.InputManager;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.TouchEvent;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.ui.Picture;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.style.ElementId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.arifolth.anjrpg.interfaces.ANJRpgInterface;
import ru.arifolth.anjrpg.interfaces.GameLogicCoreInterface;
import ru.arifolth.anjrpg.interfaces.MusicTypeEnum;
import ru.arifolth.anjrpg.interfaces.SoundTypeEnum;

import static com.simsilica.lemur.component.BorderLayout.Position.Center;
import static com.simsilica.lemur.component.BorderLayout.Position.North;

public class PressAnyKeyState extends BaseAppState implements RawInputListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PressAnyKeyState.class);

    private Container mainWindow;
    private Label titleLabel;
    private Label promptLabel;
    private ANJRpgInterface application;
    private GameLogicCoreInterface gameLogicCore;
    private InputManager inputManager;

    // Animation state
    private float fadeTimer = 0f;
    private float blinkTimer = 0f;
    private boolean promptVisible = true;
    private boolean isTransitioning = false;
    private Picture backgroundPicture;
    private Node gui;

    // Configuration
    private static final float FADE_IN_DURATION = 2.0f;
    private static final float BLINK_INTERVAL = 1.5f;

    @Override
    protected void initialize(Application app) {
        application = (ANJRpgInterface) app;
        gameLogicCore = application.getGameLogicCore();
        inputManager = application.getInputManager();

        // Initialize menu background music
        if (gameLogicCore != null && gameLogicCore.getSoundManager() != null) {
            gameLogicCore.getSoundManager().setNextMusicType(MusicTypeEnum.MENU);
        }
        gui = ((SimpleApplication) application).getGuiNode();

        int width = application.getSettings().getWidth();
        int height = application.getSettings().getHeight();
        backgroundPicture = new Picture("Background");
        backgroundPicture.setImage(gameLogicCore.getAssetManager(), "Interface/rpg_background.png", false);
        backgroundPicture.setWidth(width);
        backgroundPicture.setHeight(height);
        backgroundPicture.setLocalTranslation(0, 0, -1); // Behind UI

        // Set initial state
        fadeTimer = 0f;
        blinkTimer = 0f;
        promptVisible = true;
        isTransitioning = false;
    }

    @Override
    protected void cleanup(Application app) {
        // Final cleanup – remove listener if still attached
        if (inputManager != null) {
            inputManager.removeRawInputListener(this);
        }
    }

    @Override
    protected void onEnable() {
        // Add raw listener only when this state becomes active
        if (inputManager != null) {
            inputManager.addRawInputListener(this);
        }
        createUI();
    }

    @Override
    protected void onDisable() {
        // Remove listener to avoid processing input when state is inactive
        if (inputManager != null) {
            inputManager.removeRawInputListener(this);
        }
        // Remove UI elements
        if (mainWindow != null) {
            mainWindow.removeFromParent();
            mainWindow = null;
        }
    }

    private void createUI() {
        gui.attachChild(backgroundPicture);

        // Get screen dimensions
        int height = application.getSettings().getHeight();
        mainWindow = new Container(new BorderLayout());

        // Create title label
        titleLabel = new Label("ANJRpg", new ElementId("press-any-key-title"));
        titleLabel.setFontSize(38);
        titleLabel.setColor(new ColorRGBA(0.9f, 0.7f, 0.3f, 0f)); // Golden color, initially transparent

        // Create prompt label
        promptLabel = new Label("Press Any Key to Continue", new ElementId("press-any-key-prompt"));
        promptLabel.setFontSize(18);
        promptLabel.setColor(new ColorRGBA(0.8f, 0.8f, 0.8f, 0f)); // Light gray, initially transparent

        mainWindow.addChild(titleLabel, North);
        mainWindow.addChild(promptLabel, Center);
        mainWindow.setBackground(null);

        setWindowSize(height);

        // Add to scene
        gui.attachChild(mainWindow);
        GuiGlobals.getInstance().requestFocus(mainWindow);
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);

        if (isTransitioning)
            return;

        // Handle fade-in animation
        if (fadeTimer < FADE_IN_DURATION) {
            fadeTimer += tpf;
            float alpha = Math.min(1.0f, fadeTimer / FADE_IN_DURATION);

            // Fade in title
            ColorRGBA titleColor = titleLabel.getColor().clone();
            titleColor.a = alpha;
            titleLabel.setColor(titleColor);

            // Fade in prompt (starts after title is halfway)
            if (fadeTimer > FADE_IN_DURATION * 0.5f) {
                float promptAlpha = Math.min(1.0f, (fadeTimer - FADE_IN_DURATION * 0.5f) / (FADE_IN_DURATION * 0.5f));
                ColorRGBA promptColor = promptLabel.getColor().clone();
                promptColor.a = promptAlpha;
                promptLabel.setColor(promptColor);
            }
        } else {
            // Handle blinking animation for prompt
            blinkTimer += tpf;
            if (blinkTimer >= BLINK_INTERVAL) {
                blinkTimer = 0f;
                promptVisible = !promptVisible;

                ColorRGBA promptColor = promptLabel.getColor().clone();
                promptColor.a = promptVisible ? 0.8f : 0.2f;
                promptLabel.setColor(promptColor);
            }
        }
    }

    // RawInputListener implementations – ALL keys / mouse buttons trigger
    @Override
    public void onKeyEvent(KeyInputEvent event) {
        if (event.isPressed() && !isTransitioning && fadeTimer >= FADE_IN_DURATION) {
            LOGGER.debug("Key pressed: keyCode={}, keyChar={}", event.getKeyCode(), event.getKeyChar());
            transitionToMainMenu();
        }
    }

    @Override
    public void onTouchEvent(TouchEvent touchEvent) {
        // IGNORE touch events (mobile/tablet)
    }

    @Override
    public void onMouseButtonEvent(MouseButtonEvent event) {
        if (event.isPressed() && !isTransitioning && fadeTimer >= FADE_IN_DURATION) {
            LOGGER.debug("Mouse button pressed: buttonIndex={}", event.getButtonIndex());
            transitionToMainMenu();
        }
    }

    @Override
    public void beginInput() {}
    @Override
    public void endInput() {}
    @Override
    public void onJoyAxisEvent(com.jme3.input.event.JoyAxisEvent evt) {}
    @Override
    public void onJoyButtonEvent(com.jme3.input.event.JoyButtonEvent evt) {}
    @Override
    public void onMouseMotionEvent(com.jme3.input.event.MouseMotionEvent evt) {}

    private void transitionToMainMenu() {
        isTransitioning = true;

        // Play menu sound effect
        if (gameLogicCore != null && gameLogicCore.getSoundManager() != null) {
            gameLogicCore.getSoundManager().getSoundNode(SoundTypeEnum.MENU).play();
        }

        // Disable this state and enable main menu
        setEnabled(false);
        getStateManager().attach(new MainMenuState());
    }

    public float getStandardScale() {
        return application.getCamera().getHeight() / (application.getCamera().getHeight() / 2f);
    }

    private void setWindowSize(int height) {
        Vector3f pref = mainWindow.getPreferredSize().clone();
        float standardScale = getStandardScale();
        pref.multLocal(1.5f * standardScale);

        // Center horizontally and position with slight bias toward the top
        int width = application.getSettings().getWidth();
        float x = (width - pref.x) * 0.5f; // Center horizontally
        float y = height * 0.5f + pref.y * 0.45f;

        mainWindow.setLocalTranslation(x, y, 0);
        mainWindow.setLocalScale(1.5f * standardScale);
    }

    public Picture getBackgroundPicture() {
        return backgroundPicture;
    }
}