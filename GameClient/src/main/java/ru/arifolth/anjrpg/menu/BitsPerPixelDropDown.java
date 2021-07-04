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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BitsPerPixelDropDown extends Dropdown<String> {
    private static final int BITS_PER_PIXEL_SIZE = 2;

    protected void initialize() {
        listBox.getModel().addAll(getDepthBits());

        if(listBox.getModel().size() > 0) {
            //set default text
            setDefaultText();
        }
    }

    private Collection<String> getDepthBits() {
        List<String> bitsPerPixel = new ArrayList<>(BITS_PER_PIXEL_SIZE);

        bitsPerPixel.add(Integer.toString(16));
        bitsPerPixel.add(Integer.toString(24));
        bitsPerPixel.add(Integer.toString(32));

        return bitsPerPixel;
    }
}
