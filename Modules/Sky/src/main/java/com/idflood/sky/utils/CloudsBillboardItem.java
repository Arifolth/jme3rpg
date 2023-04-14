/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2014 - 2023 Alexander Nilov
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

package com.idflood.sky.utils;

import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.shape.Dome;

public class CloudsBillboardItem extends Geometry {
    private BillboardControl billBoadControl = new BillboardControl();
    private Mesh box;


    public CloudsBillboardItem(String name, Float scale) {
        super(name);
        box = new Dome(Vector3f.ZERO, 2, 32, 1000f,true);

        setMesh(box);

        setQueueBucket(RenderQueue.Bucket.Transparent);
        setCullHint(CullHint.Never);

        addControl(billBoadControl);

        setLocalScale(scale);

        getControl(BillboardControl.class).setAlignment(BillboardControl.Alignment.AxialY);
    }
}
