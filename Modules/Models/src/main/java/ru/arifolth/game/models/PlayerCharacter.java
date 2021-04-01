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

package ru.arifolth.game.models;

import com.jme3.animation.*;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.input.controls.ActionListener;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;

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
public class PlayerCharacter extends GameCharacter implements ActionListener, AnimEventListener {
    public static final float MAXIMUM_HEALTH = 75f;
    private final Node characterNode = new Node("Player");

    private Camera cam;
    private AnimChannel animationChannel;
    private AnimChannel attackChannel;
    private AnimControl animationControl;

    private Vector3f walkDirection = new Vector3f();
    private boolean left = false, right = false, up = false, down = false,
        attacking = false, capture_mouse = true, running = false, blocking = false, block_pressed = false,
        jumping = false, jump_pressed = false, attack_pressed = false,
        lock_movement = false;
    private float airTime = 0;
    private HealthBar healthBar;

    public PlayerCharacter() {

    }

    public void initialize(BulletAppState bulletAppState, AssetManager assetManager) {
        super.initialize(bulletAppState, assetManager);

        initializePhysixControl(bulletAppState);

        initializeCharacterModel(assetManager);

        initializeAnimation();

    }

    private void initializePhysixControl(BulletAppState bulletAppState) {
        // We set up collision detection for the characterControl by creating
        // a capsule collision shape and a CharacterControl.
        // The CharacterControl offers extra settings for
        // size, stepheight, jumping, falling, and gravity.
        // We also put the characterControl in its starting position.
        CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(1.5f, 6f, 1);
        characterControl = new CharacterControl(capsuleShape, 0.8f);
        characterControl.setJumpSpeed(20);
        characterControl.setFallSpeed(300);
        characterControl.setGravity(30);
        bulletAppState.getPhysicsSpace().add(characterControl);
    }

    private void initializeCharacterModel(AssetManager assetManager) {
        //characterModel = assetManager.loadModel("Models/Sinbad/Sinbad.mesh.xml");
        characterModel = assetManager.loadModel("Models/Ninja/Ninja.mesh.xml");
        //Material playerMaterial = new Material(assetManager, "Common/MatDefs/Misc/ShowNormals.j3md");
        //characterModel.setMaterial(playerMaterial);
        characterModel.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        //characterModel.setLocalScale(1f);
        characterModel.setLocalScale(0.055f);
        characterModel.setQueueBucket(RenderQueue.Bucket.Transparent);

        characterNode.addControl(characterControl);
        characterNode.attachChild(characterModel);

        //ninja collision sphere offset fix
        //characterModel.move(0, -5f, 0); //sinbad
        characterModel.move(0, -4.6f, 0);

        //http://jmonkeyengine.org/forum/topic/my-ninja-character-is-floating-after-i-replaced-oto-with-him/
        //characterModel.setLocalTranslation(new Vector3f(0f, 40.0f, 0f));
        //characterControl.setPhysicsLocation(characterModel.getLocalTranslation());
        //characterModel.getLocalTranslation().subtractLocal(0f, 50.0f,0f); // model offset fix

        characterModel.getControl(SkeletonControl.class).setHardwareSkinningPreferred(true);

        initializeHealthBar(assetManager);
    }

    private void initializeHealthBar(AssetManager assetManager) {
        healthBar = new HealthBar(assetManager, characterNode);
        healthBar.create();
    }

    private void initializeAnimation() {
        animationControl = characterModel.getControl(AnimControl.class);
        animationControl.addListener(this);
        animationChannel = animationControl.createChannel();
        animationChannel.setAnim("Idle3");
        attackChannel = animationControl.createChannel();

        /*attackChannel.addBone(animationControl.getSkeleton().getBone("Joint5"));
        attackChannel.addBone(animationControl.getSkeleton().getBone("Joint6"));
        attackChannel.addBone(animationControl.getSkeleton().getBone("Joint7"));
        attackChannel.addBone(animationControl.getSkeleton().getBone("Joint8"));
        attackChannel.addBone(animationControl.getSkeleton().getBone("Joint9"));
        attackChannel.addBone(animationControl.getSkeleton().getBone("Joint10"));
        attackChannel.addBone(animationControl.getSkeleton().getBone("Joint11"));
        attackChannel.addBone(animationControl.getSkeleton().getBone("Joint12"));
        attackChannel.addBone(animationControl.getSkeleton().getBone("Joint13"));
        attackChannel.addBone(animationControl.getSkeleton().getBone("Joint29"));*/
    }

    public void setCam(Camera cam) {
        this.cam = cam;
    }

    @Override
    public Node getNode() {
        return characterNode;
    }

    /** These are our custom actions triggered by key presses.
     * We do not walk yet, we just keep track of the direction the user pressed. */
    public void onAction(String binding, boolean pressed, float tpf) {
        if (binding.equals("Left")) {
            left = pressed;
        }
        else if (binding.equals("Right")) {
            right = pressed;
        }
        else if (binding.equals("Up")) {
            up = pressed;
        }
        else if (binding.equals("Down")) {
            down = pressed;
        }
        else if (binding.equals("Jump")) {
            jump_pressed = true;
        }
        else if (binding.equals("Run")) {
            running = pressed;
        }
        else if (binding.equals("Block")) {
            if(capture_mouse && !jumping) {
                block_pressed = pressed;
                if(block_pressed && !blocking) {
                    blocking = true;
                    block();
                }
            }
        } else if (binding.equals("Attack")) {
            if(capture_mouse && !jumping) {
                attack_pressed = pressed;
                if(attack_pressed && !attacking) {
                    attacking = true;
                    attack();
                }
            }
        }
    }

    private void block() {
        //TODO: Show Blocking animation only in case attack is coming, do nothing otherwise
        attackChannel.setAnim("Block", 0.1f);
        //TODO: ADD Blocking event
        attackChannel.setLoopMode(LoopMode.DontLoop);
        attackChannel.setSpeed(1f);
        attackChannel.setTime(attackChannel.getAnimMaxTime()/2);
    }

    private void attack() {
        attackChannel.setAnim("Attack3", 0.1f);
        //TODO: ADD Attacking event
        attackChannel.setLoopMode(LoopMode.DontLoop);
    }

    @Override
    public void onAnimCycleDone(AnimControl ctrl, AnimChannel ch, String name) {
        if(name.equals("Attack3") && attacking && !attack_pressed) {
            if (!ch.getAnimationName().equals("Idle3")) {
                ch.setAnim("Idle3", 0f);
                ch.setLoopMode(LoopMode.Loop);
                ch.setSpeed(1f);
                attacking = false;
                lock_movement = false;
            }
        } else if(name.equals("Block") && blocking && !block_pressed) {
            if (!ch.getAnimationName().equals("Idle3")) {
                ch.setAnim("Idle3", 0f);
                ch.setLoopMode(LoopMode.Loop);
                ch.setSpeed(1f);
                blocking = false;
                lock_movement = false;
            }
        } else if(name.equals("JumpNoHeight")) {
            jump_pressed = false;
        }

        if (ch == attackChannel) {
            ch.setAnim("Walk");
        }
    }

    @Override
    public void onAnimChange(AnimControl ctrl, AnimChannel ch, String name) {
        //TODO:
    }
    /**
     * This is the main event loop--walking happens here.
     * We check in which direction the playerControl is walking by interpreting
     * the camera direction forward (camDir) and to the side (camLeft).
     * The setWalkDirection() command is what lets a physics-controlled playerControl walk.
     * We also make sure here that the camera moves with playerControl.
     */
    @Override
    public void simpleUpdate(float k) {
        healthBarUpdate();

        movementUpdate(k);
    }

    private void healthBarUpdate() {
        healthBar.update();
    }

    private void movementUpdate(float k) {
        float movement_amount = 0.3f;
        if(running) {
            movement_amount *= 1.75;
        }

        // Gets forward direction and moves it forward
        Vector3f camDir = cam.getDirection().clone().multLocal(movement_amount);
        // Gets left direction and moves it to the left
        Vector3f camLeft = cam.getLeft().clone().multLocal(movement_amount * 0.75f);

        // We don't want to fly or go underground
        camDir.y = 0;
        camLeft.y = 0;

        walkDirection.set(0, 0, 0); // The walk direction is initially null

        if(left) { walkDirection.addLocal(camLeft); }
        if(right) { walkDirection.addLocal(camLeft.negate()); }
        if(up) { walkDirection.addLocal(camDir);
        }
        if(down) { walkDirection.addLocal(camDir.negate()); }

        if(!characterControl.onGround()) airTime += k;
        else {
            airTime = 0;
            jumping = false;
        }

        if ((airTime > 0.1f || jump_pressed) && !attacking) {
            jumping = true;
            // Stop movement if jumping while walking
            if(jump_pressed && animationChannel.getAnimationName().equals("Walk"))
                lock_movement = true;
            if (!animationChannel.getAnimationName().equals("JumpNoHeight")) {
                animationChannel.setAnim("JumpNoHeight");
                animationChannel.setSpeed(1f);
                animationChannel.setLoopMode(LoopMode.DontLoop);
            }
            if(animationChannel.getTime() >= 0.32f) { // Delay jump to make the animation look decent
                characterControl.jump();
                lock_movement = false;
            }
        } else if(blocking) {
            lock_movement = true;
            if (!animationChannel.getAnimationName().equals("Block")) {
                animationChannel.setAnim("Block");
                animationChannel.setSpeed(1f);
                animationChannel.setLoopMode(LoopMode.DontLoop);
            }
            if(!block_pressed) {
                animationChannel.setAnim("Idle3", 0f);
                animationChannel.setSpeed(1f);
                blocking = false;
                lock_movement = false;
            }
        } else if(attacking) {
            lock_movement = true;
            if (!animationChannel.getAnimationName().equals("Attack3")) {
                animationChannel.setAnim("Attack3");
                animationChannel.setSpeed(1f);
                animationChannel.setLoopMode(LoopMode.DontLoop);
            }
            if(!attack_pressed) {
                animationChannel.setAnim("Idle3", 0f);
                animationChannel.setSpeed(1f);
                attacking = false;
                lock_movement = false;
            }
        } else {
            // If we're not walking, set standing animation if not jumping
            if (walkDirection.length() == 0) {
                animationChannel.setLoopMode(LoopMode.Loop);
                if (!animationChannel.getAnimationName().equals("Idle3")) {
                    animationChannel.setAnim("Idle3", 0f);
                    animationChannel.setSpeed(1f);
                }
            } else {
                // ... otherwise, set the walking animation
                animationChannel.setLoopMode(LoopMode.Loop);
                if(!animationChannel.getAnimationName().equals("Walk"))
                    animationChannel.setAnim("Walk", 0.5f);
                if(running) animationChannel.setSpeed(1.75f);
                else animationChannel.setSpeed(1f);
            }
        }
        if(!lock_movement)
            characterControl.setWalkDirection(walkDirection);
        else
            characterControl.setWalkDirection(Vector3f.ZERO);

        // Rotate model to point walk direction if moving
        if(walkDirection.length() != 0)
            characterControl.setViewDirection(walkDirection.negate());
        // negating cause the model is flipped

        //walk backwards
        if((walkDirection.length() != 0) && down)
            characterControl.setViewDirection(walkDirection);

        // Rotate model to point camera direction if attacking
        //if(attacking)
        //characterControl.setViewDirection(direction.negate());
    }

    @Override
    public boolean isAttacking() {
        return attacking;
    }

    @Override
    public boolean isBlocking() {
        return blocking;
    }
}
