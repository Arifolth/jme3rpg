/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2014 - 2025 Alexander Nilov
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
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.ui.Picture;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.lemur.style.ElementId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.arifolth.anjrpg.interfaces.ANJRpgInterface;
import ru.arifolth.anjrpg.interfaces.GameLogicCoreInterface;
import ru.arifolth.anjrpg.interfaces.MusicTypeEnum;
import ru.arifolth.anjrpg.interfaces.SoundTypeEnum;

import static com.simsilica.lemur.component.BorderLayout.Position.Center;
import static com.simsilica.lemur.component.BorderLayout.Position.North;

public class PressAnyKeyState extends BaseAppState implements ActionListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(PressAnyKeyState.class);

    private static final String ANY_KEY_ACTION = "AnyKeyPressed";
    private Container mainWindow;
    private Label titleLabel;
    private Label promptLabel;
    private ANJRpgInterface application;
    private GameLogicCoreInterface gameLogicCore;

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

        // Set up input mappings for any key/mouse button
        setupInputMappings();

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
        backgroundPicture.setLocalTranslation(0, 0, -1);  // Behind UI

        // Set initial state
        fadeTimer = 0f;
        blinkTimer = 0f;
        promptVisible = true;
        isTransitioning = false;
    }

    private void setupInputMappings() {
        // Map common keys
        String[] keyMappings = {
                "KEY_SPACE", "KEY_RETURN", "KEY_ESCAPE", "KEY_W", "KEY_A", "KEY_S", "KEY_D",
                "KEY_UP", "KEY_DOWN", "KEY_LEFT", "KEY_RIGHT", "KEY_LSHIFT", "KEY_RSHIFT"
        };

        for (String key : keyMappings) {
            try {
                int keyCode = KeyInput.class.getField(key).getInt(null);
                application.getInputManager().addMapping(ANY_KEY_ACTION, new KeyTrigger(keyCode));
            } catch (Exception e) {
                LOGGER.debug("Could not map key: " + key);
            }
        }

        // Map mouse buttons
        application.getInputManager().addMapping(ANY_KEY_ACTION,
                new MouseButtonTrigger(MouseInput.BUTTON_LEFT),
                new MouseButtonTrigger(MouseInput.BUTTON_RIGHT),
                new MouseButtonTrigger(MouseInput.BUTTON_MIDDLE));

        application.getInputManager().addListener(this, ANY_KEY_ACTION);
    }

    @Override
    protected void cleanup(Application app) {
        // Clean up input mappings
        application.getInputManager().deleteMapping(ANY_KEY_ACTION);
        application.getInputManager().removeListener(this);
    }

    @Override
    protected void onEnable() {
        createUI();
    }

    @Override
    protected void onDisable() {
        if (mainWindow != null) {
            mainWindow.removeFromParent();
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

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (ANY_KEY_ACTION.equals(name) && isPressed && !isTransitioning) {
            // Only respond after fade-in is complete
            if (fadeTimer >= FADE_IN_DURATION) {
                transitionToMainMenu();
            }
        }
    }

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
