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

import com.jme3.system.AppSettings;
import ru.arifolth.anjrpg.interfaces.ViewDistanceSettings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ViewDistanceDropDown extends Dropdown {

    protected void initialize(AppSettings settings) {
        this.settings = settings;
        listBox.getModel().addAll(getRenderers());

        setCurrentValue();
    }

    @Override
    public String getSelectedValue() {
        Integer selectionItem = getSelectionModel().getSelection();
        if (null == selectionItem)
            selectionItem = getDefaultSelection(chosenElement.getText());
        return getModel().get(selectionItem);
    }

    protected void setCurrentValue() {
        chosenElement.setText(SettingsUtils.getViewDistanceSettings(settings).name());
    }

    private Collection<String> getRenderers() {
        List<String> renderers = new ArrayList<>();

        renderers.add(ViewDistanceSettings.LOW.name());
        renderers.add(ViewDistanceSettings.MED.name());
        renderers.add(ViewDistanceSettings.HIGH.name());

        return renderers;
    }
}
