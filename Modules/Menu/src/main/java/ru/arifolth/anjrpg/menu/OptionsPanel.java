package ru.arifolth.anjrpg.menu;

import com.jme3.app.Application;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.SpringGridLayout;
import ru.arifolth.anjrpg.interfaces.ANJRpgInterface;
import ru.arifolth.anjrpg.interfaces.SoundTypeEnum;

public class OptionsPanel implements MenuPanel {
    private Container container;
    private InGameMenuState parentMenu;
    private Application app;

    public OptionsPanel(Application app, InGameMenuState parentMenu) {
        this.app = app;
        this.parentMenu = parentMenu;

        container = GameUI.createPanelContainer();
        container.setLayout(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.None));

        Label title = container.addChild(new Label("Options"));
        title.setFontSize(32);
        title.setInsets(new Insets3f(10, 10, 0, 10));

        ActionButton back = container.addChild(new ActionButton(new CallMethodAction("Back to System", this, "back")));
        back.setInsets(new Insets3f(10, 10, 10, 10));
    }

    @Override
    public Container getContainer() { return container; }

    public void back() {
        ((ANJRpgInterface) app).getGameLogicCore().getSoundManager().getSoundNode(SoundTypeEnum.MENU).play();
        parentMenu.showPanel("System");
    }
}