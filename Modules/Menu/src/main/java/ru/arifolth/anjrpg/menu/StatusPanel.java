/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2014 - 2026 Alexander Nilov

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package ru.arifolth.anjrpg.menu;

import com.jme3.app.Application;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.component.SpringGridLayout;
import ru.arifolth.anjrpg.interfaces.ANJRpgInterface;
import ru.arifolth.anjrpg.interfaces.stats.CharacterStats;

import static com.simsilica.lemur.component.BorderLayout.Position.East;
import static com.simsilica.lemur.component.BorderLayout.Position.West;

public class StatusPanel implements MenuPanel {
    private final ANJRpgInterface application;
    private Container container;

    public StatusPanel(Application app) {
        application = (ANJRpgInterface) app;

        container = new Container(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.Even));
        container.setBackground(null);

        Label title = container.addChild(new Label("Character Status"));
        title.setFontSize(32);
        title.setInsets(new Insets3f(10, 10, 0, 10));

        buildUI();
    }

    private void addStat(Container props, String name, Object value) {
        props.setBackground(null);
        Label labelName = new Label(name + ":");
        labelName.setFontSize(24);
        props.addChild(labelName, West);
        Label labelValue = new Label(String.valueOf(value));
        props.addChild(labelValue, East);
    }

    @Override
    public void buildUI() {
        Container panelContainer = new Container();
        panelContainer.setBackground(null);
        Container joinPanel = container.addChild(panelContainer);
        joinPanel.setInsets(new Insets3f(10, 10, 10, 10));

        CharacterStats playerCharacterStats = application.getGameLogicCore().getPlayerCharacter().getStats();
        addStat(joinPanel.addChild(createContainerWithBorderLayout()), "Level", playerCharacterStats.getLevel());
        addStat(joinPanel.addChild(createContainerWithBorderLayout()), "Health", playerCharacterStats.getHealth());
        addStat(joinPanel.addChild(createContainerWithBorderLayout()), "Mana", playerCharacterStats.getMana());
        addStat(joinPanel.addChild(createContainerWithBorderLayout()), "Stamina", playerCharacterStats.getStamina());
    }

    private static Container createContainerWithBorderLayout() {
        return new Container(new BorderLayout());
    }

    @Override
    public Container getContainer() { return container; }
}