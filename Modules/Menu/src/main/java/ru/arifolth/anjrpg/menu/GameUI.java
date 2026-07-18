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

import com.simsilica.lemur.ActionButton;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Insets3f;

public class GameUI {
    public static Container createPanelContainer() {
        Container container = new Container();
        // Rely on the global "glass" theme and the PressAnyKeyState background picture
        container.setBackground(null);
        return container;
    }

    public static void styleNavButton(ActionButton button) {
        button.setInsets(new Insets3f(10, 10, 10, 10));
    }
}