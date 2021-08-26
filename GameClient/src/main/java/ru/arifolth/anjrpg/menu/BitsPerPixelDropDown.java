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

import com.jme3.system.AppSettings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BitsPerPixelDropDown extends Dropdown {
    private static final int BITS_PER_PIXEL_SIZE = 2;

    protected void initialize(AppSettings settings) {
        this.settings = settings;
        listBox.getModel().addAll(getBitsPerPixel());

        setCurrentValue();
    }

    protected void setCurrentValue() {
        int bitsPerPixel = settings.getBitsPerPixel();

        chosenElement.setText(Integer.toString(bitsPerPixel));
    }

    private Collection<String> getBitsPerPixel() {
        List<String> bitsPerPixel = new ArrayList<>(BITS_PER_PIXEL_SIZE);

        bitsPerPixel.add(Integer.toString(16));
        bitsPerPixel.add(Integer.toString(24));
        bitsPerPixel.add(Integer.toString(32));

        return bitsPerPixel;
    }
}
