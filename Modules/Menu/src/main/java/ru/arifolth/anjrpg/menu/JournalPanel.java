package ru.arifolth.anjrpg.menu;

import com.jme3.app.Application;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.SpringGridLayout;

public class JournalPanel implements MenuPanel {
    private Container container;

    public JournalPanel(Application app) {
        container = new Container(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.Even));
        container.setBackground(null);

        Label title = container.addChild(new Label("Journal"));
        title.setFontSize(32);
        title.setInsets(new Insets3f(10, 10, 0, 10));
    }

    @Override
    public Container getContainer() { return container; }
}