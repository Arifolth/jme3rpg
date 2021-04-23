/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2021 Alexander Nilov
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

package ru.arifolth.game.models;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.AnimEventListener;
import com.jme3.animation.SkeletonControl;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.debug.SkeletonDebugger;

/*
*
*  ninja.mesh

    Found in: Samples/Media/models
    Number of faces: 904
    Number of vertices: 781
    Author: Psionic, from the CharacterFX site.
    Animations: Attack1 Attack2 Attack3 Backflip Block Climb Crouch Death1 Death2 HighJump Idle1 Idle2 Idle3 Jump JumpNoHeight Kick SideKick Spin Stealth Walk
    Number of bones: 28
    Initial facing vector: Vector3::NEGATIVE_UNIT_Z
*/
public abstract class NinjaCharacter extends GameCharacter implements AnimEventListener {
    protected AnimChannel animationChannel;
    protected AnimChannel attackChannel;
    private AnimControl animationControl;

    protected void initializeCharacterModel() {
        //characterModel = assetManager.loadModel("Models/Sinbad/Sinbad.mesh.xml");
        characterModel = assetManager.loadModel("Models/Ninja/Ninja.mesh.xml");
        //Material playerMaterial = new Material(assetManager, "Common/MatDefs/Misc/ShowNormals.j3md");
        //characterModel.setMaterial(playerMaterial);
        characterModel.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        //characterModel.setLocalScale(1f);
        characterModel.setLocalScale(0.055f);
        characterModel.setQueueBucket(RenderQueue.Bucket.Transparent);

        //ninja collision sphere offset fix
        //characterModel.move(0, -5f, 0); //sinbad
        characterModel.move(0, -4.6f, 0);

        //http://jmonkeyengine.org/forum/topic/my-ninja-character-is-floating-after-i-replaced-oto-with-him/
        //characterModel.setLocalTranslation(new Vector3f(0f, 40.0f, 0f));
        //characterControl.setPhysicsLocation(characterModel.getLocalTranslation());
        //characterModel.getLocalTranslation().subtractLocal(0f, 50.0f,0f); // model offset fix

        characterModel.getControl(SkeletonControl.class).setHardwareSkinningPreferred(true);
    }

    @Override
    protected void initializeAnimation() {
        animationControl = characterModel.getControl(AnimControl.class);
        animationControl.addListener(this);
        animationChannel = animationControl.createChannel();
        animationChannel.setAnim("Idle3");
        attackChannel = animationControl.createChannel();
        attackChannel.setAnim("Idle3");

        //add appropriate bones to an attack channels, so character could walk and attack simultaneously
        //top layer - body, spine, shoulders, arms
        attackChannel.addFromRootBone("Joint3");

        //bottom layer - spine end, legs
        animationChannel.addBone("Joint2");
        animationChannel.addFromRootBone("Joint18");
        animationChannel.addFromRootBone("Joint23");
    }

    @Override
    protected void initializeSkeletonDebug() {
        //debug skeleton
        SkeletonDebugger skeletonDebug = new SkeletonDebugger("skeleton", animationControl.getSkeleton());
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.getAdditionalRenderState().setWireframe(true);
        mat.setColor("Color", ColorRGBA.Blue);
        mat.getAdditionalRenderState().setDepthTest(false);
        skeletonDebug.setMaterial(mat);
        ((Node)characterModel).attachChild(skeletonDebug);
    }

    @Override
    public void onAnimChange(AnimControl ctrl, AnimChannel ch, String name) {}
}
