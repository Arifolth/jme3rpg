/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2022 Alexander Nilov
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RendererDropDown extends Dropdown {

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
        String renderer = settings.getRenderer();

        chosenElement.setText(renderer);
    }

    private Collection<String> getRenderers() {
        List<String> renderers = new ArrayList<>();

        renderers.add(AppSettings.LWJGL_OPENGL2);
        renderers.add(AppSettings.LWJGL_OPENGL30);
        renderers.add(AppSettings.LWJGL_OPENGL31);
        renderers.add(AppSettings.LWJGL_OPENGL32);
        renderers.add(AppSettings.LWJGL_OPENGL33);
        renderers.add(AppSettings.LWJGL_OPENGL40);
        renderers.add(AppSettings.LWJGL_OPENGL41);
        renderers.add(AppSettings.LWJGL_OPENGL42);
        renderers.add(AppSettings.LWJGL_OPENGL43);
        renderers.add(AppSettings.LWJGL_OPENGL44);
        renderers.add(AppSettings.LWJGL_OPENGL45);

        return renderers;
    }
}
