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
import com.jme3.system.AppSettings;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.component.SpringGridLayout;
import ru.arifolth.anjrpg.interfaces.ANJRpgInterface;
import ru.arifolth.anjrpg.interfaces.Constants;
import ru.arifolth.anjrpg.interfaces.FilterManagerInterface;
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
 * FilterManager.initialize()) and FSR CAS must stay before GammaCorrection,
 * and removeFilter/addFilter would move them to the end of the chain.</p>
 *
 * <p>From the main menu the world (and with it the FilterManager) is not
 * loaded yet, so Apply only persists the keys there - FilterManager reads
 * them back when it creates the filters, and the toggles hold across the
 * Start Game that follows.</p>
 *
 * <p>The checkboxes carry their own text and are added directly to the
 * join panel, exactly like VideoPanel's Fullscreen/VSync rows: every row
 * starts at the same left edge in one SpringGridLayout column, so the
 * check indicators line up.</p>
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

        joinPanel.addChild(filmGrainCheckbox);
        joinPanel.addChild(fsrCASCheckbox);

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
        }

        AppSettings settings = ((ANJRpgInterface) application).getSettings();
        settings.putBoolean(Constants.ADVANCED_VIDEO_FILM_GRAIN, filmGrainCheckbox.isChecked());
        settings.putBoolean(Constants.ADVANCED_VIDEO_FSR_CAS, fsrCASCheckbox.isChecked());
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
