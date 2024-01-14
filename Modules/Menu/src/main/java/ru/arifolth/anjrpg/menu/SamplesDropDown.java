/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2014 - 2024 Alexander Nilov
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

public class SamplesDropDown extends Dropdown {

    protected void initialize(AppSettings settings) {
        this.settings = settings;
        listBox.getModel().addAll(getSamles());

        setCurrentValue();
    }

    protected void setCurrentValue() {
        int samples = settings.getSamples();

        chosenElement.setText(Integer.toString(samples));
    }

    private Collection<String> getSamles() {
        List<String> samples = new ArrayList<>();

        samples.add(Integer.toString(0));
        samples.add(Integer.toString(1));
        samples.add(Integer.toString(2));
        samples.add(Integer.toString(4));
        samples.add(Integer.toString(8));
        samples.add(Integer.toString(16));

        return samples;
    }
}
