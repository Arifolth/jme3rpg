/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2014 - 2023 Alexander Nilov
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

import com.simsilica.lemur.Container;
import com.simsilica.state.CompositeAppState;

public abstract class CustomCompositeAppState extends CompositeAppState {
    protected OptionsMenuState parent;
    protected Container window;

    public CustomCompositeAppState(OptionsMenuState parent) {
        this.parent = parent;
    }

    public OptionsMenuState getParent() {
        return parent;
    }

    public Container getMainWindow() {
        return window;
    }
}
