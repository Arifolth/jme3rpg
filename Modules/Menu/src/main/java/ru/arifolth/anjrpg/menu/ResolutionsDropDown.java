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

import com.jme3.app.Application;
import com.jme3.system.AppSettings;

import java.awt.*;
import java.util.*;
import java.util.List;

public class ResolutionsDropDown extends Dropdown {
    private static final int WIDTH_LIMIT = 3840;
    private static final int HEIGHT_LIMIT = 2160;
    private static final int minWidth = 1280;
    private static final int minHeight = 1024;
    // Array of supported display modes
    private DisplayMode[] modes = null;

    protected void initialize(AppSettings settings) {
        this.settings = settings;
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

        modes = device.getDisplayModes();
        Arrays.sort(modes, new DisplayModeSorter());
        listBox.getModel().addAll(getResolutions(modes, WIDTH_LIMIT, HEIGHT_LIMIT));

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
        int currentWidth = settings.getWidth();
        int currentHeight = settings.getHeight();
        String res = currentWidth + " x " + currentHeight;

        chosenElement.setText(res);
    }

    /**
     * Returns every unique resolution from an array of <code>DisplayMode</code>s
     * where the resolution is greater than the configured minimums.
     */
    private java.util.List<String> getResolutions(DisplayMode[] modes, int widthLimit, int heightLimit) {
        List<String> resolutions = new ArrayList<>(modes.length);
        for (DisplayMode mode : modes) {
            int height = mode.getHeight();
            int width = mode.getWidth();
            if (width >= minWidth && height >= minHeight) {
                if (height >= heightLimit) {
                    height = heightLimit;
                }
                if (width >= widthLimit) {
                    width = widthLimit;
                }

                String res = width + " x " + height;
                if (!resolutions.contains(res)) {
                    resolutions.add(res);
                }
            }
        }
        return resolutions;
    }

    /**
     * Utility class for sorting <code>DisplayMode</code>s. Sorts by
     * resolution, then bit depth, and then finally refresh rate.
     */
    private class DisplayModeSorter implements Comparator<DisplayMode> {

        /**
         * @see Comparator#compare(Object, Object)
         */
        public int compare(DisplayMode a, DisplayMode b) {
            // Width
            if (a.getWidth() != b.getWidth()) {
                return (a.getWidth() > b.getWidth()) ? 1 : -1;
            }
            // Height
            if (a.getHeight() != b.getHeight()) {
                return (a.getHeight() > b.getHeight()) ? 1 : -1;
            }
            // Bit depth
            if (a.getBitDepth() != b.getBitDepth()) {
                return (a.getBitDepth() > b.getBitDepth()) ? 1 : -1;
            }
            // Refresh rate
            if (a.getRefreshRate() != b.getRefreshRate()) {
                return (a.getRefreshRate() > b.getRefreshRate()) ? 1 : -1;
            }
            // All fields are equal
            return 0;
        }
    }
}
