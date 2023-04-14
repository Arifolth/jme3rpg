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

import com.jme3.system.AppSettings;
import ru.arifolth.anjrpg.interfaces.Constants;

import java.util.*;
import java.util.List;

public class FrameRateDropDown extends Dropdown {
    public static final int FRAME_RATES_SIZE = 2;

    @Override
    protected void initialize(AppSettings settings) {
        this.settings = settings;
        listBox.getModel().addAll(getRefreshRates());

        setCurrentValue();
    }

    protected void setCurrentValue() {
        int frameRate = settings.getFrameRate();
        if(frameRate < Constants.MIN_FRAME_RATE) {
            frameRate = Constants.MIN_FRAME_RATE;
        }
        chosenElement.setText(Integer.toString(frameRate));
    }

    private java.util.List<String> getRefreshRates() {
        List<String> refreshRates = new ArrayList<>(FRAME_RATES_SIZE);

        refreshRates.add(Integer.toString(Constants.MIN_FRAME_RATE));
        refreshRates.add(Integer.toString(60));

        return refreshRates;
    }
}
