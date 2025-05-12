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

package ru.arifolth.anjrpg;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;

public class RenderingThreadPriorityAppState extends BaseAppState {

    @Override
    protected void initialize(Application app) {
        app.enqueue(() -> {
            // Access the rendering thread
            Thread renderingThread = Thread.currentThread();

            // Set maximum priority for the rendering thread
            renderingThread.setPriority(Thread.MAX_PRIORITY);
        });
    }

    @Override
    protected void cleanup(Application app) {
        // Cleanup if necessary
    }

    @Override
    protected void onEnable() {
        // Handle when the state is enabled
    }

    @Override
    protected void onDisable() {
        // Handle when the state is disabled
    }
}
