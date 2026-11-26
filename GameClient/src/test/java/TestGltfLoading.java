/*
 * Copyright (c) 2009-2022 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import com.jme3.anim.AnimComposer;
import com.jme3.anim.Armature;
import com.jme3.anim.Joint;
import com.jme3.anim.SkinningControl;
import com.jme3.app.ChaseCameraAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Limits;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.jme3.scene.debug.custom.ArmatureDebugAppState;
import com.jme3.scene.plugins.gltf.GltfModelKey;
import com.jme3.system.AppSettings;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class TestGltfLoading extends SimpleApplication {

    final private Node autoRotate = new Node("autoRotate");
    final private List<Spatial> assets = new ArrayList<>();
    private Node probeNode;
    private final static String indentString = "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t";
    private boolean playAnim = true;
    private Node handAttach;
    private Node leftArmAttach;

    public static void main(String[] args) {
        TestGltfLoading app = new TestGltfLoading();
        AppSettings settings = new AppSettings(true);
        settings.setFullscreen(false);
        settings.setResolution(3840, 2160);
        app.setSettings(settings);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        ArmatureDebugAppState armatureDebugappState = new ArmatureDebugAppState();
        getStateManager().attach(armatureDebugappState);

        String folder = System.getProperty("user.home");
        assetManager.registerLocator(folder, FileLocator.class);

        cam.setFrustumPerspective(45f, (float) cam.getWidth() / cam.getHeight(), 0.1f, 100f);
        renderer.setDefaultAnisotropicFilter(Math.min(renderer.getLimits().get(Limits.TextureAnisotropy), 8));
        setPauseOnLostFocus(false);

        flyCam.setMoveSpeed(5);
        flyCam.setDragToRotate(true);
        flyCam.setEnabled(false);
        viewPort.setBackgroundColor(new ColorRGBA().setAsSrgb(0.2f, 0.2f, 0.2f, 1.0f));
        rootNode.attachChild(autoRotate);
        probeNode = (Node) assetManager.loadModel("assets/Scenes/defaultProbe.j3o");
        autoRotate.attachChild(probeNode);

        Spatial knight = loadModel("assets/Models/Characters/stechhelmkn/Meshy_AI_stechhelm_knight_remesh_20261123_animated_mixamo_All_animation_20.glb", new Vector3f(0, -1.2f, 0), null);
        Spatial shield  = loadModel("assets/Models/Characters/stechhelmkn/Meshy_AI_jousting_targe_lowpoly_game_asset_20261126_texture.glb", new Vector3f(0, 0, 0), new Vector3f(0.6f, 0.6f, 0.6f) );
        Spatial sword  = loadModel("assets/Models/Characters/stechhelmkn/Meshy_AI_Boden_Knight_Sword_Upward_Low_Poly_20261125_texture.glb", new Vector3f(0, 0, 0), null);

        for(Spatial spatial: assets)
            probeNode.attachChild(spatial);

        // --- SHIELD ATTACHMENT FIX START ---

        assets.remove(shield);

        SkinningControl knightSkin = findControl(knight, SkinningControl.class);

        if (knightSkin != null) {
            Node leftArmAttach = knightSkin.getAttachmentsNode("mixamorig:LeftForeArm");

            if (leftArmAttach != null) {
                Node shieldOffset = new Node("ShieldOffset");

                // 1. Translation: Slide it DOWN the arm and closer to the body
                // X: Less negative brings it closer to the torso.
                // Y: Negative slides it down towards the hand (Mixamo bones point down).
                // Z: Pushes it slightly forward so it doesn't clip the elbow.
                shield.setLocalTranslation(-0.05f, -0.15f, 0.05f);
                shield.setLocalRotation(Quaternion.IDENTITY);

                // 2. Rotation: Keep this exactly as it is! It works perfectly.
                shieldOffset.setLocalRotation(new Quaternion().fromAngles(
                        -FastMath.PI,    // X: Keeps it upright
                        0,         // Y: Faces it outward
                        0     // Z: Tilts it
                ));

                shieldOffset.attachChild(shield);
                leftArmAttach.attachChild(shieldOffset);

                System.out.println("Shield attached to Left Forearm successfully.");
            }
        }
        // --- SHIELD ATTACHMENT FIX END ---

        // --- SWORD ATTACHMENT FIX START ---

        assets.remove(sword);

        Node swordOffset = new Node("SwordOffset");

        sword.setLocalRotation(Quaternion.IDENTITY);
        sword.setLocalTranslation(-0.060f, 0.34f, 0.0125f);

        // Current rotation (twisted clockwise)
        swordOffset.setLocalRotation(new Quaternion().fromAngles(0f, -0.75f, -FastMath.HALF_PI));

        swordOffset.attachChild(sword);
        handAttach.attachChild(swordOffset);

        // --- SWORD ATTACHMENT FIX END ---

        ChaseCameraAppState chaseCam = new ChaseCameraAppState();
        chaseCam.setTarget(probeNode);
        getStateManager().attach(chaseCam);
        chaseCam.setInvertHorizontalAxis(true);
        chaseCam.setInvertVerticalAxis(true);
        chaseCam.setZoomSpeed(0.5f);
        chaseCam.setMinVerticalRotation(-FastMath.HALF_PI);
        chaseCam.setRotationSpeed(3);
        chaseCam.setDefaultDistance(3);
        chaseCam.setDefaultVerticalRotation(0.3f);

        // --- INPUT MAPPING ---

        // Map SPACE to trigger the next animation
        inputManager.addMapping("nextAnim", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (isPressed && composer != null) {
                    String anim = anims.poll();
                    if (anim != null) {
                        anims.add(anim);
                        composer.setCurrentAction(anim);
                        System.out.println("Playing animation: " + anim);
                    }
                }
            }
        }, "nextAnim");

        // Map ENTER to toggle play/pause of animations
        inputManager.addMapping("toggleAnim", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (isPressed) {
                    playAnim = !playAnim;
                    if (playAnim) {
                        playFirstAnim(rootNode);
                    } else {
                        stopAnim(rootNode);
                    }
                }
            }
        }, "toggleAnim");

        dumpScene(rootNode, 0);
    }

    private <T extends Control> T findControl(Spatial s, Class<T> controlClass) {
        T ctrl = s.getControl(controlClass);
        if (ctrl != null) {
            return ctrl;
        }
        if (s instanceof Node) {
            Node n = (Node) s;
            for (Spatial spatial : n.getChildren()) {
                ctrl = findControl(spatial, controlClass);
                if (ctrl != null) {
                    return ctrl;
                }
            }
        }
        return null;
    }

    private Spatial loadModel(String path, Vector3f offset, Vector3f scale) {
        GltfModelKey k = new GltfModelKey(path);
        Spatial model = assetManager.loadModel(k);
        if(null != scale) {
            model.scale(scale.x, scale.y, scale.z);
            model.setLocalScale(scale.x, scale.y, scale.z);
        }
        model.move(offset);
        assets.add(model);
        if (playAnim) {
            playFirstAnim(model);
        }

        SkinningControl ctrl = findControl(model, SkinningControl.class);

        if (ctrl != null) {
            ctrl.setHardwareSkinningPreferred(true);
            Armature armature = ctrl.getArmature();
            for (Joint joint : armature.getJointList()) {
                System.out.println("Joint: " + joint.getName());
            }

            String rightHandJoint = "mixamorig:RightHand";
            handAttach = ctrl.getAttachmentsNode(rightHandJoint);

            leftArmAttach = ctrl.getAttachmentsNode("mixamorig:LeftForeArm");
        }
        return model;
    }

    final private Queue<String> anims = new LinkedList<>();
    private AnimComposer composer;

    private void playFirstAnim(Spatial s) {
        AnimComposer control = s.getControl(AnimComposer.class);
        if (control != null) {
            anims.clear();
            for (String name : control.getAnimClipsNames()) {
                anims.add(name);
                System.out.println("AnimClip: " + name);
            }
            if (anims.isEmpty()) {
                return;
            }
            // Start with the first animation (or default to "Running" if it exists)
            String anim = null;
            if (control.getAnimClipsNames().contains("Idle_4")) {
                anim = "Idle_4";
                // Re-order the queue so Running is at the front
                anims.remove("Idle_4");
                anims.add("Idle_4");
            }

            if(anim == null)
                anim = anims.poll();

            control.setCurrentAction(anim);
            composer = control;
        }
        if (s instanceof Node) {
            Node n = (Node) s;
            for (Spatial spatial : n.getChildren()) {
                playFirstAnim(spatial);
            }
        }
    }

    private void stopAnim(Spatial s) {
        AnimComposer control = s.getControl(AnimComposer.class);
        if (control != null) {
            control.reset();
        }
        if (s instanceof Node) {
            Node n = (Node) s;
            for (Spatial spatial : n.getChildren()) {
                stopAnim(spatial);
            }
        }
    }

    // simpleUpdate no longer cycles models
    @Override
    public void simpleUpdate(float tpf) {
        // Left empty for manual animation testing
    }

    private void dumpScene(Spatial s, int indent) {
        System.err.println(indentString.substring(0, indent) + s.getName() + " (" + s.getClass().getSimpleName() + ") / " +
                s.getLocalTransform().getTranslation().toString() + ", " +
                s.getLocalTransform().getRotation().toString() + ", " +
                s.getLocalTransform().getScale().toString());
        if (s instanceof Node) {
            Node n = (Node) s;
            for (Spatial spatial : n.getChildren()) {
                dumpScene(spatial, indent + 1);
            }
        }
    }
}
