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

package ru.arifolth.anjrpg.interfaces.core;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface ThreadExecutorProvider {
    Logger LOGGER = Logger.getLogger(ThreadExecutorProvider.class.getName());

    static ExecutorService getExecutor() {
        try {
            Method method = Executors.class.getMethod("newVirtualThreadPerTaskExecutor");
            LOGGER.log(Level.INFO,"ExecutorService: newVirtualThreadPerTaskExecutor");
            return (ExecutorService) method.invoke(null);
        } catch (ReflectiveOperationException e) {
            LOGGER.log(Level.INFO,"ExecutorService: newWorkStealingPool");
            // Method not found or invocation failed, fallback to regular executor
            return Executors.newWorkStealingPool();
        }
    }
}
