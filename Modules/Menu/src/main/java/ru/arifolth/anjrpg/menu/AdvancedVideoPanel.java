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
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.Camera;
import com.jme3.system.AppSettings;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.component.SpringGridLayout;
import ru.arifolth.anjrpg.interfaces.ANJRpgInterface;
import ru.arifolth.anjrpg.interfaces.Constants;
import ru.arifolth.anjrpg.interfaces.FilterManagerInterface;
import ru.arifolth.anjrpg.interfaces.ResolutionPreset;
import ru.arifolth.anjrpg.interfaces.SoundTypeEnum;

import static com.simsilica.lemur.component.BorderLayout.Position.East;
import static com.simsilica.lemur.component.BorderLayout.Position.West;

/**
 * Runtime on/off switches for the GPU post-processing filters. Reached two
 * ways, like VideoPanel: in-game through System -> Advanced Video
 * (parentPanel != null) and from the main menu through Options ->
 * Advanced Video (parentPanel == null, wrapped by AdvancedVideoMenuState).
 *
 * <p>Toggles go through {@code Filter.setEnabled(boolean)}, which makes
 * FilterPostProcessor skip the pass on the next rendered frame without
 * touching the filter's index in the chain. The filters are never
 * removed/re-added: Film Grain must stay last (added after Water in
 * FilterManager.initialize()) and FSR CAS must stay immediately after the
 * FSR EASU upscaler, and removeFilter/addFilter would move them to the end
 * of the chain.</p>
 *
 * <p>The FSR Upscaling section adds, above the two existing checkboxes:</p>
 * <ul>
 *   <li>"FSR Upscaling" - master toggle for the low-resolution render path
 *       + EASU upscaler. Default off; the whole chain renders natively
 *       while off.</li>
 *   <li>"Upscaling Preset" - quality preset dropdown, RELATIVE to the
 *       current display resolution (Ultra Quality ~0.77x, Quality ~0.67x,
 *       Balanced ~0.59x, Performance ~0.50x - see ResolutionPreset). The
 *       internal resolution is recomputed whenever the display resolution
 *       changes, so no absolute-resolution list is offered (a fixed list
 *       breaks on lower/equal display resolutions).</li>
 *   <li>"Internal Resolution" - read-only label with the effective
 *       internal -&gt; display resolution, e.g. "Quality - 2560x1440 -&gt;
 *       3840x2160". Updates live when the checkbox or the preset
 *       changes.</li>
 * </ul>
 *
 * <p>From the main menu the world (and with it the FilterManager) is not
 * loaded yet, so Apply only persists the keys there - FilterManager reads
 * them back when it creates the filters, and the toggles hold across the
 * Start Game that follows. The computed-resolution label still works there
 * because it is derived from the preset and the live display size alone.</p>
 *
 * <p>Future filters follow the same pattern: one Checkbox field, one
 * {@code joinPanel.addChild(...)}, one {@code Constants.ADVANCED_VIDEO_*}
 * key, one setEnabled line in {@link #apply()} and one setEnabled line in
 * FilterManager's setup.</p>
 */
public class AdvancedVideoPanel implements MenuPanel {
    private final Application application;
    private final SystemPanel parentPanel;
    private final Container container;

    private final Checkbox filmGrainCheckbox = new Checkbox("Film Grain");
    private final Checkbox fsrCASCheckbox    = new Checkbox("FSR CAS Sharpening");
    private final Checkbox fsrUpscalingCheckbox = new Checkbox("FSR Upscaling");
    private final UpscalingPresetDropDown upscalingPresetDropDown = new UpscalingPresetDropDown();
    private final Label upscalingResolutionLabel = new Label("");

    /** Inactive label tint: greyed out while upscaling is off (spec 11.2, rule 4). */
    private static final ColorRGBA LABEL_ACTIVE_COLOR = new ColorRGBA(1f, 1f, 1f, 1f);
    private static final ColorRGBA LABEL_INACTIVE_COLOR = new ColorRGBA(0.55f, 0.55f, 0.55f, 1f);

    public AdvancedVideoPanel(Application app, SystemPanel parentPanel) {
        this.application = app;
        this.parentPanel = parentPanel;

        container = GameUI.createPanelContainer();
        container.setLayout(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.None));

        Label title = container.addChild(new Label("Advanced Video Settings"));
        title.setFontSize(32);
        title.setInsets(new Insets3f(10, 10, 0, 10));

        initialize();
        buildUI();
    }

    @Override
    public void buildUI() {
        Container joinPanel = container.addChild(new Container());
        joinPanel.setBackground(null);
        joinPanel.setInsets(new Insets3f(10, 10, 10, 10));

        // --- FSR Upscaling section (spec section 11.1) ---
        joinPanel.addChild(fsrUpscalingCheckbox);

        Container props = joinPanel.addChild(new Container(new BorderLayout()));
        props.setBackground(null);
        props.addChild(new Label("Upscaling Preset:"), West);
        props.addChild(upscalingPresetDropDown, East);

        props = joinPanel.addChild(new Container(new BorderLayout()));
        props.setBackground(null);
        props.addChild(new Label("Internal Resolution:"), West);
        props.addChild(upscalingResolutionLabel, East);

        // --- Existing toggles ---
        joinPanel.addChild(fsrCASCheckbox);
        joinPanel.addChild(filmGrainCheckbox);

        Container applyRow = joinPanel.addChild(new Container(new BorderLayout()));
        applyRow.setBackground(null);
        applyRow.addChild(new ActionButton(new CallMethodAction("Apply", this, "apply")), West);

        // Add Back button only for Main Menu context (when parentPanel is null)
        if (parentPanel == null) {
            applyRow.addChild(new ActionButton(new CallMethodAction("Back", this, "back")), East);
        }
    }

    private void initialize() {
        AppSettings settings = ((ANJRpgInterface) application).getSettings();
        filmGrainCheckbox.setChecked(SettingsUtils.readToggle(settings, Constants.ADVANCED_VIDEO_FILM_GRAIN));
        fsrCASCheckbox.setChecked(SettingsUtils.readToggle(settings, Constants.ADVANCED_VIDEO_FSR_CAS));
        // Default OFF (unlike the two toggles above): absent key = native path.
        fsrUpscalingCheckbox.setChecked(SettingsUtils.readToggle(settings, Constants.ADVANCED_VIDEO_UPSCALING, false));
        upscalingPresetDropDown.initialize(settings);

        // Live label updates: checkbox flips and preset picks both refresh
        // the computed-resolution label immediately (spec 11.2, rule 4/7).
        fsrUpscalingCheckbox.addClickCommands(button -> updateUpscalingUiState());
        upscalingPresetDropDown.setOnSelectionChange(this::updateUpscalingUiState);
        updateUpscalingUiState();
    }

    /**
     * Refreshes the read-only "Internal Resolution" label from the current
     * checkbox/dropdown state and the LIVE display resolution. The label is
     * the notification channel required by spec 11.2, rule 3 (fallbacks are
     * reported through it) - e.g. "Quality - 2560x1440 -> 3840x2160", or a
     * note that the preset is unavailable at the current display size.
     */
    private void updateUpscalingUiState() {
        int[] display = currentDisplayResolution();
        if (!fsrUpscalingCheckbox.isChecked()) {
            upscalingResolutionLabel.setText("Off - renders at " + display[0] + "x" + display[1]);
            upscalingResolutionLabel.setColor(LABEL_INACTIVE_COLOR);
            return;
        }
        ResolutionPreset preset = upscalingPresetDropDown.getSelectedPreset();
        upscalingResolutionLabel.setText(preset.describe(display[0], display[1]));
        upscalingResolutionLabel.setColor(LABEL_ACTIVE_COLOR);
    }

    /**
     * The live display (framebuffer) resolution - the target EASU upscales
     * to, whatever it currently is (4K, 2K, ultrawide, windowed, ...). The
     * presets are RATIOS of this size, so no resolution is ever hardcoded:
     * the context settings track window resizes, the application-level
     * settings hold the resolution picked in Video settings, and the camera
     * is the last resort (the actual canvas size). Never a fixed size: the
     * upscaling chain derives its target silently from whatever the current
     * screen resolution is.
     */
    private int[] currentDisplayResolution() {
        if (application.getContext() != null && application.getContext().getSettings() != null) {
            AppSettings contextSettings = application.getContext().getSettings();
            if (contextSettings.getWidth() > 0 && contextSettings.getHeight() > 0) {
                return new int[]{contextSettings.getWidth(), contextSettings.getHeight()};
            }
        }
        AppSettings settings = ((ANJRpgInterface) application).getSettings();
        if (settings != null && settings.getWidth() > 0 && settings.getHeight() > 0) {
            return new int[]{settings.getWidth(), settings.getHeight()};
        }
        Camera camera = application.getCamera();
        if (camera != null && camera.getWidth() > 0 && camera.getHeight() > 0) {
            return new int[]{camera.getWidth(), camera.getHeight()};
        }
        // Truly nothing available (not expected in practice): fall back to
        // the smallest preset-friendly size rather than a fixed 4K.
        return new int[]{1280, 720};
    }

    private void apply() {
        ((ANJRpgInterface) application).getSoundManager()
            .getSoundNode(SoundTypeEnum.MENU).play();

        FilterManagerInterface fm = ((ANJRpgInterface) application)
            .getGameLogicCore().getFilterManager();

        // Null in the main menu before the world is loaded: the keys are
        // persisted below and FilterManager applies them when it creates
        // the filters, so the toggle still takes effect after Start Game.
        if (fm != null) {
            fm.getFilmGrainFilter().setEnabled(filmGrainCheckbox.isChecked());
            fm.getFsrCASFilter().setEnabled(fsrCASCheckbox.isChecked());
            // Preset first, then the master switch: setUpscalingEnabled
            // re-applies the currently selected preset, so this order ends
            // with exactly one effective reconfiguration.
            fm.applyResolutionPreset(upscalingPresetDropDown.getSelectedPreset());
            fm.setUpscalingEnabled(fsrUpscalingCheckbox.isChecked());
        }

        AppSettings settings = ((ANJRpgInterface) application).getSettings();
        settings.putBoolean(Constants.ADVANCED_VIDEO_FILM_GRAIN, filmGrainCheckbox.isChecked());
        settings.putBoolean(Constants.ADVANCED_VIDEO_FSR_CAS, fsrCASCheckbox.isChecked());
        settings.putBoolean(Constants.ADVANCED_VIDEO_UPSCALING, fsrUpscalingCheckbox.isChecked());
        SettingsUtils.writeUpscalingPreset(settings, upscalingPresetDropDown.getSelectedPreset());
        SettingsUtils.saveSettings(settings);
    }

    private void back() {
        ((ANJRpgInterface) application).getSoundManager().getSoundNode(SoundTypeEnum.MENU).play();
        if (parentPanel == null) {
            // Main Menu Context: Disable OptionsMenuState to return to Main Menu root
            OptionsMenuState optionsMenu = (OptionsMenuState) application.getStateManager().getState(OptionsMenuState.class);
            if (optionsMenu != null) {
                optionsMenu.setEnabled(false);
            }
        }
    }

    @Override
    public Container getContainer() { return container; }

    @Override
    public void onDeactivated() { /* no resources to release */ }
}
