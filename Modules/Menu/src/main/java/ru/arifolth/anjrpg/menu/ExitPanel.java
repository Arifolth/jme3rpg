/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2014 - 2026 Alexander Nilov
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

import com.jme3.app.Application;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.SpringGridLayout;
import ru.arifolth.anjrpg.interfaces.ANJRpgInterface;
import ru.arifolth.anjrpg.interfaces.SoundTypeEnum;

public class ExitPanel implements MenuPanel {
    private Container container;
    private Application app;
    private InGameMenuState parentMenu;

    public ExitPanel(Application app, InGameMenuState parentMenu) {
        this.app = app;
        this.parentMenu = parentMenu;

        container = GameUI.createPanelContainer();
        container.setLayout(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.None));

        Label title = container.addChild(new Label("Exit Game?"));
        title.setFontSize(32);
        title.setInsets(new Insets3f(10, 10, 0, 10));

        ActionButton yes = container.addChild(new ActionButton(new CallMethodAction("Yes", this, "exitGame")));
        yes.setInsets(new Insets3f(10, 10, 10, 10));

        ActionButton no = container.addChild(new ActionButton(new CallMethodAction("No", this, "back")));
        no.setInsets(new Insets3f(10, 10, 10, 10));
    }

    @Override
    public Container getContainer() { return container; }

    public void exitGame() {
        ((ANJRpgInterface) app).getGameLogicCore().getSoundManager().getSoundNode(SoundTypeEnum.MENU).play();
        app.stop();
    }

    public void back() {
        ((ANJRpgInterface) app).getGameLogicCore().getSoundManager().getSoundNode(SoundTypeEnum.MENU).play();
        // Return to the System/Options panel when "No" is clicked
        parentMenu.showPanel("System");
    }
}