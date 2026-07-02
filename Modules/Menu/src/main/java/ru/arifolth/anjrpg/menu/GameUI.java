package ru.arifolth.anjrpg.menu;

import com.simsilica.lemur.ActionButton;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Insets3f;

public class GameUI {
    public static Container createPanelContainer() {
        Container container = new Container();
        // Rely on the global "glass" theme and the PressAnyKeyState background picture
        container.setBackground(null);
        return container;
    }

    public static void styleNavButton(ActionButton button) {
        button.setInsets(new Insets3f(10, 10, 10, 10));
    }
}