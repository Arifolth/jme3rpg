/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2014 - 2025 Alexander Nilov
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

package ru.arifolth.anjrpg.camera;

import com.jme3.input.InputManager;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import ru.arifolth.anjrpg.interfaces.camera.FollowCameraInterface;

public class FreeFollowCamera extends AbstractControl implements FollowCameraInterface {
    private static final String MOUSE_WHEEL_UP = "MOUSE_WHEEL_UP";
    private static final String MOUSE_WHEEL_DOWN = "MOUSE_WHEEL_DOWN";
    private static final float MIN_ZOOM = 2.0f;
    private static final float MAX_ZOOM = 25.0f;

    private final Vector3f worldUp = Vector3f.UNIT_Y;
    private float currentYaw = 0.0f;
    private float currentPitch = 0.3f;
    private final float maxPitch = FastMath.QUARTER_PI; // 45 degrees max pitch
    private final float minPitch = -FastMath.QUARTER_PI; // -45 degrees min pitch
    // New rotation angles

    private final Camera cam;
    private final Spatial target;
    //camera position, behind the right player shoulder
    private Vector3f offset = new Vector3f(8f, 6f, 10f);
    private final InputManager inputManager;
    private float rotationSpeed = 2.0f;
    private boolean enabled = true;
    private boolean dragToRotate = false;
    private boolean canRotate = false;

    // Input mappings
    private static final String[] mappings = new String[]{
            "CUSTOM_CAM_LEFT", "CUSTOM_CAM_RIGHT",
            "CUSTOM_CAM_UP", "CUSTOM_CAM_DOWN",
            "CUSTOM_CAM_ROTATEDRAG",
            MOUSE_WHEEL_UP, MOUSE_WHEEL_DOWN  // Add zoom mappings
    };

    public FreeFollowCamera(Camera cam, Spatial target, InputManager inputManager) {
        this.cam = cam;
        this.target = target;
        this.inputManager = inputManager;

        registerWithInput();
    }

    private void registerWithInput() {
        inputManager.addMapping("CUSTOM_CAM_LEFT", new MouseAxisTrigger(MouseInput.AXIS_X, true));
        inputManager.addMapping("CUSTOM_CAM_RIGHT", new MouseAxisTrigger(MouseInput.AXIS_X, false));
        inputManager.addMapping("CUSTOM_CAM_UP", new MouseAxisTrigger(MouseInput.AXIS_Y, false));
        inputManager.addMapping("CUSTOM_CAM_DOWN", new MouseAxisTrigger(MouseInput.AXIS_Y, true));
        inputManager.addMapping("CUSTOM_CAM_ROTATEDRAG", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));

        // Add mouse wheel zoom mappings
        inputManager.addMapping(MOUSE_WHEEL_UP, new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
        inputManager.addMapping(MOUSE_WHEEL_DOWN, new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));

        inputManager.addListener(this, mappings);
        inputManager.setCursorVisible(dragToRotate);
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (enabled && target != null) {
            // Get target position and add vertical offset
            Vector3f targetPos = target.getWorldTranslation().add(0, offset.y, 0);

            // Calculate orbit position using spherical coordinates
            float horizontalDistance = offset.z * FastMath.cos(currentPitch);
            float verticalDistance = offset.z * FastMath.sin(currentPitch);
            Vector3f orbitOffset = new Vector3f(
                    horizontalDistance * FastMath.sin(currentYaw),
                    verticalDistance,
                    horizontalDistance * FastMath.cos(currentYaw)
            );

            // Base camera position (without shoulder offset)
            Vector3f desiredPos = targetPos.add(orbitOffset);

            // Calculate shoulder offset direction
            Vector3f lookDir = targetPos.subtract(desiredPos).normalize();
            Vector3f right = lookDir.cross(worldUp).normalize();

            // Apply shoulder offset
            desiredPos = desiredPos.add(right.mult(offset.x));

            // Smooth interpolation
            Vector3f currentPos = cam.getLocation();
            Vector3f newPos = currentPos.interpolateLocal(desiredPos, tpf * 5.0f);
            cam.setLocation(newPos);

            // Always look at the target
            cam.lookAt(targetPos, worldUp);
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        // Not needed for camera control
    }

    @Override
    public void onAnalog(String name, float value, float tpf) {
        if (!enabled)
            return;

        if (dragToRotate && !canRotate)
            return;

        // Inverted horizontal axis
        if (name.equals("CUSTOM_CAM_LEFT")) {
            currentYaw += rotationSpeed * value;  // Inverted: was -
        } else if (name.equals("CUSTOM_CAM_RIGHT")) {
            currentYaw -= rotationSpeed * value;  // Inverted: was +
        }
        // Inverted vertical axis
        else if (name.equals("CUSTOM_CAM_UP")) {
            currentPitch -= rotationSpeed * value;  // Inverted: was +
            currentPitch = FastMath.clamp(currentPitch, minPitch, maxPitch);
        } else if (name.equals("CUSTOM_CAM_DOWN")) {
            currentPitch += rotationSpeed * value;  // Inverted: was -
            currentPitch = FastMath.clamp(currentPitch, minPitch, maxPitch);
        }

        // Handle zoom (unchanged)
        if (name.equals(MOUSE_WHEEL_UP)) {
            zoomCamera(value);
        } else if (name.equals(MOUSE_WHEEL_DOWN)) {
            zoomCamera(-value);
        }
    }

    private void zoomCamera(float amount) {
        float zoomChange = amount * 0.5f;
        offset.z = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, offset.z - zoomChange));
    }

    @Override
    public void onAction(String name, boolean value, float tpf) {
        if (!enabled)
            return;

        if (name.equals("CUSTOM_CAM_ROTATEDRAG") && dragToRotate) {
            canRotate = value;
            inputManager.setCursorVisible(!value);
        }
    }

    // Getters and setters
    public void setOffset(Vector3f offset) {
        this.offset = offset.clone();
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setDragToRotate(boolean dragToRotate) {
        this.dragToRotate = dragToRotate;
        if (inputManager != null) {
            inputManager.setCursorVisible(dragToRotate);
        }
    }

    public void setRotationSpeed(float speed) {
        this.rotationSpeed = speed;
    }
}
