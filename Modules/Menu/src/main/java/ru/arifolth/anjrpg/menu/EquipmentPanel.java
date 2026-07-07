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
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.SpringGridLayout;

public class EquipmentPanel implements MenuPanel {
    private Container container;

    public EquipmentPanel(Application app) {
        container = new Container(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.Even));
        container.setBackground(null);

        Label title = container.addChild(new Label("Equipment"));
        title.setFontSize(32);
        title.setInsets(new Insets3f(10, 10, 0, 10));

        addSection("Armaments (Right Hand)");
        addSection("Armaments (Left Hand)");
        addSection("Armor");
        addSection("Talismans");
        addSection("Quick Items");
    }

    private void addSection(String sectionName) {
        Label sectionLabel = container.addChild(new Label(sectionName));
        sectionLabel.setFontSize(24);
        sectionLabel.setInsets(new Insets3f(10, 10, 0, 10));

        Label empty = container.addChild(new Label("[Empty Slot]"));
        empty.setInsets(new Insets3f(5, 20, 5, 20));
    }

    @Override
    public Container getContainer() { return container; }
}