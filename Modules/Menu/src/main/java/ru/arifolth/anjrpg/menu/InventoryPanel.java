package ru.arifolth.anjrpg.menu;

import com.jme3.app.Application;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.SpringGridLayout;

public class InventoryPanel implements MenuPanel {
    private Container container;

    public InventoryPanel(Application app) {
        container = new Container(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.Even));
        container.setBackground(null);

        Label title = container.addChild(new Label("Inventory"));
        title.setFontSize(32);
        title.setInsets(new Insets3f(10, 10, 0, 10));

        Label desc = container.addChild(new Label("Tabs for All Items, Crafting, Key Items..."));
        desc.setInsets(new Insets3f(10, 20, 10, 20));
    }

    @Override
    public Container getContainer() { return container; }
}