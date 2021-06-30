/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2021 Alexander Nilov
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

import com.jme3.math.Vector3f;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.Panel;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.BoxLayout;
import com.simsilica.lemur.core.GuiControl;
import com.simsilica.lemur.core.VersionedList;
import com.simsilica.lemur.core.VersionedReference;
import com.simsilica.lemur.event.PopupState;
import com.simsilica.lemur.list.SelectionModel;
import com.simsilica.lemur.style.ElementId;

import java.awt.*;
import java.util.List;
import java.util.*;


/*
* Author b00n
* https://hub.jmonkeyengine.org/t/dropdown-with-lemur/38115/23
*
* */

public class Dropdown<T> extends Panel {
    public static final String ELEMENT_ID = "dropdown";
    public static final int WIDTH_LIMIT = 3840;
    public static final int HEIGHT_LIMIT = 2160;
    private boolean popupShown;
    private final Label chosenElement;
    private final Button collapseButton;
    private final ListBox<T> listBox;
    private VersionedReference<Set<Integer>> selectionRef;

    private int minWidth = 1280;
    private int minHeight = 1024;

    // Array of supported display modes
    private DisplayMode[] modes = null;
    private boolean opened;

    public Dropdown() {
        this(null);

        initialize();
    }

    private void initialize() {
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

        modes = device.getDisplayModes();
        Arrays.sort(modes, new DisplayModeSorter());
        listBox.getModel().addAll((Collection<? extends T>) getResolutions(modes, WIDTH_LIMIT, HEIGHT_LIMIT));

        if(listBox.getModel().size() > 0) {
            //set default text
            setDefaultText();
        }
    }

    private void setDefaultText() {
        chosenElement.setText(listBox.getModel().get(getDefaultSelection()).toString());
    }

    public int getDefaultSelection() {
        return listBox.getModel().size() - 1;
    }

    public Dropdown(String style) {
        this(true, new ElementId(ELEMENT_ID), style);
    }

    public Dropdown(ElementId elementId, String style) {
        this(true, elementId, style);
    }

    /**
     * Returns every unique resolution from an array of <code>DisplayMode</code>s
     * where the resolution is greater than the configured minimums.
     */
    private List<String> getResolutions(DisplayMode[] modes, int widthLimit, int heightLimit) {
        List<String> resolutions = new ArrayList<String>(modes.length);
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

    @Override
    public void updateLogicalState(float tpf) {
        super.updateLogicalState(tpf);
        if (selectionRef.update()) {
            resetOpen();
        }
    }

    protected Dropdown(boolean applyStyles, ElementId elementId, String style) {
        super(false, elementId, style);
        BoxLayout layout = new BoxLayout(Axis.X, FillMode.None);
        getControl(GuiControl.class).setLayout(layout);

        this.collapseButton = new Button("V", elementId.child("button"), style);
        this.chosenElement = new Label("", elementId.child("selection"), style);
        layout.addChild(chosenElement);
        layout.addChild(collapseButton);
        collapseButton.setTextVAlignment(VAlignment.Center);
        collapseButton.addClickCommands((s) -> {
            resetOpen();
        });
        chosenElement.setTextHAlignment(HAlignment.Center);
        chosenElement.setTextVAlignment(VAlignment.Center);
        listBox = new ListBox<>(new VersionedList<>(), elementId.child("popup"), style);

        this.selectionRef = listBox.getSelectionModel().createReference();
        if (applyStyles) {
            GuiGlobals.getInstance().getStyles().applyStyles(this, elementId, style);
        }
    }

    protected void resetOpen() {
        final PopupState popupState = GuiGlobals.getInstance().getPopupState();
        if (popupShown) {
            if (popupState.isPopup(listBox)) {
                popupState.closePopup(listBox);
            }
            popupShown = false;
        } else {
            Vector3f preferredSize = new Vector3f(Math.max(getSize().x, 250f), 500f, 0f);
            listBox.setSize(preferredSize);
            listBox.setPreferredSize(preferredSize);
            listBox.setLocalScale(2.5f);
            Vector3f localTranslation = getWorldTranslation();
            listBox.setLocalTranslation(localTranslation.x, localTranslation.y - getSize().y, localTranslation.z);
            popupState.showPopup(listBox, (source) -> {
                Integer selection = listBox.getSelectionModel().getSelection();
                if (selection != null) {
                    chosenElement.setText(listBox.getModel().get(selection).toString());
                }
                popupShown = false;
            });
            GuiGlobals.getInstance().requestFocus(listBox);
            popupShown = true;
        }
    }

    public VersionedList<T> getModel() {
        return listBox.getModel();
    }

    public SelectionModel getSelectionModel() {
        return listBox.getSelectionModel();
    }

    /**
     * Utility class for sorting <code>DisplayMode</code>s. Sorts by
     * resolution, then bit depth, and then finally refresh rate.
     */
    private class DisplayModeSorter implements Comparator<DisplayMode> {

        /**
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
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
