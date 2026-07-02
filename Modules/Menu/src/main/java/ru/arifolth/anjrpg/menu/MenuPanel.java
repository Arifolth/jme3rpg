package ru.arifolth.anjrpg.menu;

import com.simsilica.lemur.Container;

public interface MenuPanel {
    Container getContainer();
    default void onActivated() {}
    default void onDeactivated() {}
}