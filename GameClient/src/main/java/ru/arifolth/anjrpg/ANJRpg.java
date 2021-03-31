/**
 *     Copyright (C) 2021  Alexander Nilov
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

package ru.arifolth.anjrpg;

import com.jme3.math.Vector3f;
import com.jme3.niftygui.NiftyJmeDisplay;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.Controller;
import de.lessvoid.nifty.controls.Parameters;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.input.NiftyInputEvent;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;

import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.jme3.niftygui.NiftyJmeDisplay.newNiftyJmeDisplay;

/*
* https://ev1lbl0w.github.io/jme-wiki-pt-pt/jme3/advanced/loading_screen.html
* */
public class ANJRpg extends RolePlayingGame implements ScreenController, Controller {

    private NiftyJmeDisplay niftyDisplay;
    private Nifty nifty;
    private Boolean load = false;
    private ScheduledExecutorService exec = Executors.newScheduledThreadPool(2);
    private Future loadFuture = null;
    final private static Logger LOGGER = Logger.getLogger(ANJRpg.class.getName());

    public static void main(String[] args) {
        app = new ANJRpg();
        app.start();
    }

    @Override
    public void simpleInitApp()  {
        super.simpleInitApp();

        niftyDisplay = newNiftyJmeDisplay(assetManager,
                inputManager,
                audioRenderer,
                guiViewPort);
        nifty = niftyDisplay.getNifty();

        nifty.fromXml("Interface/nifty_loading.xml", "start", this);

        guiViewPort.addProcessor(niftyDisplay);

        showLoadingMenu();
    }

    @Override
    public void simpleUpdate(float tpf) {
        if(null == load) {
            super.simpleUpdate(tpf);
            return;
        }

        if (load) {
            if (loadFuture == null) {
                //if we have not started loading, submit Callable to executor
                loadFuture = exec.submit(loadingCallable);
            }
            //check if the execution on the other thread is done
            if (loadFuture.isDone()) {
                try {
                    countDownLatch.await(3L, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    LOGGER.log(Level.SEVERE, "Resources did not loaded properly: ", e.getStackTrace());
                } finally {
                    attachPlayer();
                    attachTerrain();
                    attachSky();
                }

                //wait until land appears in Physics Space
                if(bulletAppState.getPhysicsSpace().getRigidBodyList().size() == 4) {
                    //put player at the beginning location
                    getGameLogicCore().getPlayerCharacter().getCharacterControl().setPhysicsLocation(new Vector3f(0, -15, 0));

                    //these calls have to be done on the update loop thread,
                    //especially attaching the terrain to the rootNode
                    //after it is attached, it's managed by the update loop thread
                    // and may not be modified from any other thread anymore!
                    nifty.gotoScreen("end");
                    nifty.exit();
                    guiViewPort.removeProcessor(niftyDisplay);
                    load = null;

                    createMinimap();
                }
            }
        }
    }

    //This is the callable that contains the code that is run on the other
    //thread.
    //Since the assetmananger is threadsafe, it can be used to load data from
    //any thread.
    //We do *not* attach the objects to the rootNode here!
    Callable<Void> loadingCallable = new Callable<Void>() {

        @Override
        public Void call() throws InterruptedException {
            Element element = nifty.getScreen("loadlevel").findElementById("loadingtext");
            textRenderer = element.getRenderer(TextRenderer.class);

            loadResources();

            setProgress("Loading complete");

            return null;
        }
    };

    public void showLoadingMenu() {
        nifty.gotoScreen("loadlevel");
        load = true;
    }

    @Override
    public void onStartScreen() {
    }

    @Override
    public void onEndScreen() {
    }

    @Override
    public void bind(Nifty nifty, Screen screen) {
        progressBarElement = nifty.getScreen("loadlevel").findElementById("progressbar");
    }

    // methods for Controller
    @Override
    public boolean inputEvent(final NiftyInputEvent inputEvent) {
        return false;
    }

    @Override
    public void onFocus(boolean getFocus) {
    }

    @Override
    public void destroy() {
        super.destroy();
        shutdownAndAwaitTermination(exec);
    }

    //standard shutdown process for executor
    private void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(6, TimeUnit.SECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(6, TimeUnit.SECONDS)) {
                    LOGGER.log(Level.SEVERE, "Pool did not terminate {0}", pool);
                }
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void bind(Nifty nifty, Screen screen, Element elmnt,
                     Parameters prmtrs) {
        progressBarElement = elmnt.findElementById("progressbar");
    }

    @Override
    public void init(Parameters prmtrs) {
    }

}
