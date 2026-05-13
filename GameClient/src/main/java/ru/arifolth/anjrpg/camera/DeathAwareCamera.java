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

package ru.arifolth.anjrpg.camera;

import com.jme3.input.InputManager;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Spatial;
import ru.arifolth.anjrpg.interfaces.camera.DeathAwareCameraInterface;

public class DeathAwareCamera extends EnhancedFreeFollowCamera implements DeathAwareCameraInterface {
    private boolean deathCameraMode = false;
    private Vector3f deathObservationPoint = new Vector3f();
    private Vector3f deathCameraPosition = new Vector3f();
    private boolean isTransitioningToDeathCam = false;
    private float deathCamTransitionTime = 0f;
    private static final float DEATH_CAM_TRANSITION_DURATION = 5.0f;

    private final Vector3f tempCurrentPos = new Vector3f();
    private final Vector3f tempTargetPos = new Vector3f();

    public DeathAwareCamera(Camera cam, Spatial target, InputManager inputManager) {
        super(cam, target, inputManager);
    }

    /**
     * Activates death camera mode - camera observes the death location
     * @param deathLocation The ground position where player died
     */
    @Override
    public void activateDeathCamera(Vector3f deathLocation) {
        if (deathCameraMode)
            return;

        deathCameraMode = true;
        isTransitioningToDeathCam = true;
        deathCamTransitionTime = 0f;

        deathObservationPoint.set(deathLocation);

        Vector3f currentCamPos = cam.getLocation();
        deathCameraPosition.set(currentCamPos).addLocal(5f, 10f, 30f);
    }

    /**
     * Deactivates death camera mode and returns to normal following
     */
    @Override
    public void deactivateDeathCamera() {
        deathCameraMode = false;
        isTransitioningToDeathCam = false;
        deathCamTransitionTime = 0f;
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (!enabled)
            return;

        if (deathCameraMode) {
            updateDeathCamera(tpf);
        } else if (target != null) {
            updateNormalCamera(tpf);
        }
    }

    private void updateDeathCamera(float tpf) {
        if (isTransitioningToDeathCam) {
            deathCamTransitionTime += tpf;
            float progress = Math.min(deathCamTransitionTime / DEATH_CAM_TRANSITION_DURATION, 1.0f);

            // Reuse vectors instead of cloning
            tempCurrentPos.set(cam.getLocation());
            tempTargetPos.interpolateLocal(tempCurrentPos, deathCameraPosition, progress);
            cam.setLocation(tempTargetPos);

            cam.lookAt(deathObservationPoint, worldUp);

            if (progress >= 1.0f) {
                isTransitioningToDeathCam = false;
            }
        } else {
            // Only update if camera position changed
            if (!cam.getLocation().equals(deathCameraPosition)) {
                cam.setLocation(deathCameraPosition);
            }
            cam.lookAt(deathObservationPoint, worldUp);
        }
    }
    private void updateNormalCamera(float tpf) {
        Vector3f targetPos = target.getWorldTranslation().add(0, offset.y, 0);

        // ... your orbit calculation unchanged ...
        float horizontalDistance = offset.z * FastMath.cos(currentPitch);
        float verticalDistance = offset.z * FastMath.sin(currentPitch);
        Vector3f orbitOffset = new Vector3f(
                horizontalDistance * FastMath.sin(currentYaw),
                verticalDistance,
                horizontalDistance * FastMath.cos(currentYaw)
        );
        Vector3f desiredPos = targetPos.add(orbitOffset);
        Vector3f lookDir = targetPos.subtract(desiredPos).normalize();
        Vector3f right = lookDir.cross(worldUp).normalize();
        desiredPos = desiredPos.add(right.mult(offset.x));

        // CRITICAL FIX 1: CLAMP desiredPos to max distance (20 units)
        float targetDistance = targetPos.distance(desiredPos);
        if (targetDistance > 20.0f) {
            Vector3f directionToTarget = targetPos.subtract(desiredPos).normalizeLocal();
            desiredPos = targetPos.subtract(directionToTarget.mult(20.0f));
        }

        // CRITICAL FIX 2: Normalize interpolation alpha to prevent overshoot
        float alpha = Math.min(1.0f, tpf * 5.0f); // Cap at 1.0, no overshoot possible

        // EMERGENCY RESET during extreme lag escape
        Vector3f currentPos = cam.getLocation();
        if (currentPos.distance(targetPos) > 40.0f) { // Double max distance
            cam.setLocation(desiredPos);
            cam.lookAt(targetPos, worldUp);
            return;
        }

        Vector3f newPos = currentPos.interpolateLocal(desiredPos, alpha);
        cam.setLocation(newPos);
        cam.lookAt(targetPos, worldUp);
    }

    // Disable input during death camera mode
    @Override
    public void onAnalog(String name, float value, float tpf) {
        if (deathCameraMode) return; // No input during death camera
        super.onAnalog(name, value, tpf);
    }

    @Override
    public void onAction(String name, boolean value, float tpf) {
        if (deathCameraMode) return; // No input during death camera
        super.onAction(name, value, tpf);
    }
}
