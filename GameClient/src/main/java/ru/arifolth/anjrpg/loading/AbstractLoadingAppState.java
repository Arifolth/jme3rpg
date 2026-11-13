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


package ru.arifolth.anjrpg.loading;

import com.jme3.app.state.BaseAppState;

/**
 * Base class for the loading screen app states. Supplies the empty
 * {@code onEnable()} / {@code onDisable()} lifecycle hooks that
 * {@link BaseAppState} requires but the loading screen does not use.
 */
abstract class AbstractLoadingAppState extends BaseAppState {

    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
    }
}
