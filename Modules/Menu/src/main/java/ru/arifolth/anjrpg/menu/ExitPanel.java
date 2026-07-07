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