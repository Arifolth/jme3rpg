package ru.arifolth.anjrpg.menu;

import com.jme3.app.Application;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.SpringGridLayout;

public class EquipmentPanel implements MenuPanel {
    private Container container;

    public EquipmentPanel(Application app) {
        container = new Container(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.Even));
        container.setBackground(null);

        Label title = container.addChild(new Label("Equipment"));
        title.setFontSize(32);
        title.setInsets(new Insets3f(10, 10, 0, 10));

        addSection("Armaments (Right Hand)");
        addSection("Armaments (Left Hand)");
        addSection("Armor");
        addSection("Talismans");
        addSection("Quick Items");
    }

    private void addSection(String sectionName) {
        Label sectionLabel = container.addChild(new Label(sectionName));
        sectionLabel.setFontSize(24);
        sectionLabel.setInsets(new Insets3f(10, 10, 0, 10));

        Label empty = container.addChild(new Label("[Empty Slot]"));
        empty.setInsets(new Insets3f(5, 20, 5, 20));
    }

    @Override
    public Container getContainer() { return container; }
}