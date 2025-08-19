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

/**
 * Enhanced FreeFollowCamera with Multiple Smoothing Techniques
 *
 * This enhanced version of FreeFollowCamera class provides multiple
 * smoothing algorithms to fix camera jumping issues on uneven terrain.
 **/

package ru.arifolth.anjrpg.camera;

import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.InputManager;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.FastMath;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import ru.arifolth.anjrpg.interfaces.camera.FollowCameraInterface;

public class EnhancedFreeFollowCamera extends AbstractControl implements FollowCameraInterface, AnalogListener, ActionListener {

    public enum SmoothingType {
        LINEAR_INTERPOLATION,
        EXPONENTIAL_SMOOTHING,
        DAMPED_SPRING,
        LOW_PASS_FILTER,
        ADAPTIVE_SMOOTHING
    }

    private static final String MOUSE_WHEEL_UP = "MOUSE_WHEEL_UP";
    private static final String MOUSE_WHEEL_DOWN = "MOUSE_WHEEL_DOWN";
    private static final float MIN_ZOOM = 2.0f;
    private static final float MAX_ZOOM = 20.0f;

    // Smoothing configuration
    private SmoothingType smoothingType = SmoothingType.EXPONENTIAL_SMOOTHING;
    private float smoothingFactor = 0.1f;        // For exponential smoothing
    private float springStiffness = 100.0f;      // For spring system
    private float springDamping = 20.0f;         // For spring system
    private float cutoffFrequency = 0.5f;        // For low-pass filter
    private final float adaptiveThreshold = 1.0f;      // For adaptive smoothing

    // Camera smoothing state
    private final Vector3f smoothedPosition = new Vector3f();
    private final Vector3f velocity = new Vector3f();

    // Existing fields
    protected Vector3f worldUp = Vector3f.UNIT_Y;
    protected float currentYaw = 0.0f;
    protected float currentPitch = 0.3f;
    private final float maxPitch = FastMath.QUARTER_PI; // 45 degrees max pitch
    private final float minPitch = -FastMath.QUARTER_PI; // -45 degrees min pitch

    protected final Camera cam;
    protected final Spatial target;
    //camera position, behind the right player shoulder
    protected Vector3f offset = new Vector3f(8f, 6f, 10f);
    private final InputManager inputManager;
    private float rotationSpeed = 2.0f;
    protected boolean enabled = true;
    private boolean dragToRotate = false;
    private boolean canRotate = false;

    // Terrain collision fields
    private boolean useTerrainCollision = true;
    private final float minHeightAboveTerrain = 2.0f;
    private Spatial terrainSpatial;

    // Input mappings
    private static final String[] mappings = new String[]{
            "CUSTOM_CAM_LEFT", "CUSTOM_CAM_RIGHT",
            "CUSTOM_CAM_UP", "CUSTOM_CAM_DOWN",
            "CUSTOM_CAM_ROTATEDRAG",
            MOUSE_WHEEL_UP, MOUSE_WHEEL_DOWN
    };

    public EnhancedFreeFollowCamera(Camera cam, Spatial target, InputManager inputManager) {
        this.cam = cam;
        this.target = target;
        this.inputManager = inputManager;
        this.smoothedPosition.set(cam.getLocation());

        registerWithInput();
    }

    // Setters for smoothing configuration
    public void setSmoothingType(SmoothingType type) {
        this.smoothingType = type;
    }

    public void setSmoothingFactor(float factor) {
        this.smoothingFactor = FastMath.clamp(factor, 0.01f, 1.0f);
    }

    public void setSpringParameters(float stiffness, float damping) {
        this.springStiffness = stiffness;
        this.springDamping = damping;
    }

    public void setCutoffFrequency(float frequency) {
        this.cutoffFrequency = frequency;
    }

    public void setTerrainSpatial(Spatial terrain) {
        this.terrainSpatial = terrain;
    }

    public void setUseTerrainCollision(boolean use) {
        this.useTerrainCollision = use;
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (!enabled)
            return; // Early exit if disabled

        if (target != null) {
            // Calculate desired camera position
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

            // Apply terrain collision if enabled
            if (useTerrainCollision && terrainSpatial != null) {
                desiredPos = applyTerrainCollision(desiredPos);
            }

            // Apply smoothing based on selected algorithm
            Vector3f smoothedPos = applySmoothingAlgorithm(desiredPos, tpf);

            cam.setLocation(smoothedPos);
            cam.lookAt(targetPos, worldUp);
        }
    }

    /**
     * Apply terrain collision detection and adjustment
     */
    private Vector3f applyTerrainCollision(Vector3f desiredPos) {
        // Cast ray downward from desired position
        Ray ray = new Ray(desiredPos.add(0, 10, 0), Vector3f.UNIT_Y.negate());
        CollisionResults results = new CollisionResults();

        terrainSpatial.collideWith(ray, results);

        if (results.size() > 0) {
            CollisionResult closest = results.getClosestCollision();
            float terrainHeight = closest.getContactPoint().y;

            // Ensure minimum height above terrain
            if (desiredPos.y < terrainHeight + minHeightAboveTerrain) {
                desiredPos.y = terrainHeight + minHeightAboveTerrain;
            }
        }

        return desiredPos;
    }

    /**
     * Apply selected smoothing algorithm
     */
    private Vector3f applySmoothingAlgorithm(Vector3f targetPos, float tpf) {
        switch (smoothingType) {
            case LINEAR_INTERPOLATION:
                return applyLinearInterpolation(targetPos, tpf);
            case EXPONENTIAL_SMOOTHING:
                return applyExponentialSmoothing(targetPos, tpf);
            case DAMPED_SPRING:
                return applyDampedSpring(targetPos, tpf);
            case LOW_PASS_FILTER:
                return applyLowPassFilter(targetPos, tpf);
            case ADAPTIVE_SMOOTHING:
                return applyAdaptiveSmoothing(targetPos, tpf);
            default:
                return targetPos;
        }
    }

    /**
     * Linear Interpolation (LERP) - Simple and predictable
     */
    private Vector3f applyLinearInterpolation(Vector3f targetPos, float tpf) {
        float lerpFactor = smoothingFactor * tpf * 60.0f; // Normalize for 60 FPS
        lerpFactor = FastMath.clamp(lerpFactor, 0.0f, 1.0f);

        smoothedPosition.interpolateLocal(targetPos, lerpFactor);
        return smoothedPosition;
    }

    /**
     * Exponential Smoothing - Adapts to movement patterns
     */
    private Vector3f applyExponentialSmoothing(Vector3f targetPos, float tpf) {
        float alpha = 1.0f - FastMath.exp(-smoothingFactor * tpf * 60.0f);

        smoothedPosition.interpolateLocal(targetPos, alpha);
        return smoothedPosition;
    }

    /**
     * Damped Spring System - Most natural feeling
     */
    private Vector3f applyDampedSpring(Vector3f targetPos, float tpf) {
        // Calculate spring force
        Vector3f displacement = targetPos.subtract(smoothedPosition);
        Vector3f springForce = displacement.mult(springStiffness);

        // Calculate damping force
        Vector3f dampingForce = velocity.mult(-springDamping);

        // Calculate total force and acceleration
        Vector3f totalForce = springForce.add(dampingForce);
        Vector3f acceleration = totalForce; // Assume unit mass

        // Update velocity and position
        velocity.addLocal(acceleration.mult(tpf));
        smoothedPosition.addLocal(velocity.mult(tpf));

        return smoothedPosition;
    }

    /**
     * Low-Pass Filter - Excellent noise reduction
     */
    private Vector3f applyLowPassFilter(Vector3f targetPos, float tpf) {
        float rc = 1.0f / (2.0f * FastMath.PI * cutoffFrequency);
        float alpha = tpf / (rc + tpf);

        smoothedPosition.interpolateLocal(targetPos, alpha);
        return smoothedPosition;
    }

    /**
     * Adaptive Smoothing - Adjusts based on movement speed
     */
    private Vector3f applyAdaptiveSmoothing(Vector3f targetPos, float tpf) {
        // Calculate movement speed
        float distance = targetPos.distance(smoothedPosition);
        float speed = distance / tpf;

        // Adjust smoothing factor based on speed
        float adaptiveFactor = smoothingFactor;
        if (speed > adaptiveThreshold) {
            adaptiveFactor = FastMath.clamp(smoothingFactor * 2.0f, 0.1f, 1.0f);
        }

        float alpha = 1.0f - FastMath.exp(-adaptiveFactor * tpf * 60.0f);
        smoothedPosition.interpolateLocal(targetPos, alpha);

        return smoothedPosition;
    }

    private void registerWithInput() {
        inputManager.addMapping("CUSTOM_CAM_LEFT", new MouseAxisTrigger(MouseInput.AXIS_X, true));
        inputManager.addMapping("CUSTOM_CAM_RIGHT", new MouseAxisTrigger(MouseInput.AXIS_X, false));
        inputManager.addMapping("CUSTOM_CAM_UP", new MouseAxisTrigger(MouseInput.AXIS_Y, false));
        inputManager.addMapping("CUSTOM_CAM_DOWN", new MouseAxisTrigger(MouseInput.AXIS_Y, true));
        inputManager.addMapping("CUSTOM_CAM_ROTATEDRAG", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping(MOUSE_WHEEL_UP, new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
        inputManager.addMapping(MOUSE_WHEEL_DOWN, new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));

        inputManager.addListener(this, mappings);
        inputManager.setCursorVisible(dragToRotate);
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
        } else if (name.equals(MOUSE_WHEEL_UP)) {
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
