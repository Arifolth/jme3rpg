package ru.arifolth.anjrpg.menu;

import com.jme3.app.Application;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.SpringGridLayout;

public class StatusPanel implements MenuPanel {
    private Container container;

    public StatusPanel(Application app) {
        container = new Container(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.Even));
        container.setBackground(null);

        Label title = container.addChild(new Label("Status"));
        title.setFontSize(32);
        title.setInsets(new Insets3f(10, 10, 0, 10));

        Label stats = container.addChild(new Label("Level: 1 | HP: 100 | FP: 50 | Stamina: 100"));
        stats.setInsets(new Insets3f(10, 20, 10, 20));
    }

    @Override
    public Container getContainer() { return container; }
}