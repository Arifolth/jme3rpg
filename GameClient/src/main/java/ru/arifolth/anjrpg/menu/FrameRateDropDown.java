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

import java.util.*;
import java.util.List;

public class FrameRateDropDown extends Dropdown {

    public static final int REFRESH_RATES = 2;

    @Override
    protected void initialize() {
        listBox.getModel().addAll(getRefreshRates());

        if(listBox.getModel().size() > 0) {
            //set default text
            setDefaultText();
        }
    }

    private java.util.List<String> getRefreshRates() {
        List<String> resolutions = new ArrayList<>(REFRESH_RATES);

        resolutions.add(Integer.toString(30));
        resolutions.add(Integer.toString(60));

        return resolutions;
    }
}
