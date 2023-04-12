package jme3;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.shape.Box;

public class CameraTest extends SimpleApplication {
    @Override
    public void simpleInitApp() {
        Mesh m = new Arrow(Vector3f.UNIT_X);
        Geometry geom = new Geometry("X",m);
        geom.scale(5);
        Material mat = new Material(assetManager,"Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Red);
        geom.setMaterial(mat);
        rootNode.attachChild(geom);
        m = new Arrow(Vector3f.UNIT_Y);
        geom = new Geometry("Y",m);
        geom.scale(5);
        mat = new Material(assetManager,"Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Green);
        geom.setMaterial(mat);
        rootNode.attachChild(geom);
        m = new Arrow(Vector3f.UNIT_Z);
        geom = new Geometry("Z",m);
        geom.scale(5);
        mat = new Material(assetManager,"Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Blue);
        geom.setMaterial(mat);
        rootNode.attachChild(geom);

        m = new Box(1, 1, 1);
        geom = new Geometry("Box", m);
        mat = new Material(assetManager,"Common/MatDefs/Misc/Unshaded.j3md");
        ColorRGBA bgColor = ColorRGBA.White;
        mat.setColor("Color", bgColor);
        geom.setMaterial(mat);
        rootNode.attachChild(geom);

        cam.setLocation(new Vector3f(10,10,10));
        Quaternion rotation = new Quaternion().fromAngleAxis(45*FastMath.DEG_TO_RAD,Vector3f.UNIT_Y);
        cam.setRotation(cam.getRotation().mult(rotation));
        rotation = new Quaternion().fromAngleAxis(35*FastMath.DEG_TO_RAD,Vector3f.UNIT_X);
        cam.setRotation(cam.getRotation().mult(rotation));
        cam.setParallelProjection(true);
        float aspect = (float) cam.getWidth() / cam.getHeight();
        float size = 15;
        cam.setFrustum(-1000, 1000, -aspect * size, aspect * size, size, -size);
    }

    public static void main(String[] args) {
        CameraTest app = new CameraTest();
        app.start();
    }
}