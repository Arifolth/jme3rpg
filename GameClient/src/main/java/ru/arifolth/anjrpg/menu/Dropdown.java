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
import com.jme3.system.AppSettings;
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

import java.util.*;


/*
* Author b00n
* https://hub.jmonkeyengine.org/t/dropdown-with-lemur/38115/23
*
* */

public abstract class Dropdown extends Panel {
    public static final String ELEMENT_ID = "dropdown";
    private boolean popupShown;
    private final Button collapseButton;
    protected final Label chosenElement;
    protected final ListBox<String> listBox;
    private VersionedReference<Set<Integer>> selectionRef;
    protected AppSettings settings;

    public Dropdown() {
        this(null);
    }

    protected abstract void initialize(AppSettings settings);
    protected abstract void setCurrentValue();

    public int getDefaultSelection(Object value) {
        int i = 0;
        if(value instanceof Integer) {
            while (i < listBox.getModel().size()) {
                if((Integer) value == Integer.parseInt(listBox.getModel().get(i))) {
                    getSelectionModel().setSelection(i);
                    break;
                }

                i++;
            }
        } else if (value instanceof String) {
            while (i < listBox.getModel().size()) {
                if(value.equals(listBox.getModel().get(i))) {
                    getSelectionModel().setSelection(i);
                    break;
                }

                i++;
            }
        }

        return i;
    }

    public Dropdown(String style) {
        this(true, new ElementId(ELEMENT_ID), style);
    }

    public Dropdown(ElementId elementId, String style) {
        this(true, elementId, style);
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

    public String getSelectedValue() {
        Integer selectionItem = getSelectionModel().getSelection();
        if (null == selectionItem)
            selectionItem = getDefaultSelection(Integer.valueOf(chosenElement.getText()));
        return getModel().get(selectionItem);
    }

    public VersionedList<String> getModel() {
        return listBox.getModel();
    }

    public SelectionModel getSelectionModel() {
        return listBox.getSelectionModel();
    }
}
