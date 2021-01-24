package clouds;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

import java.nio.FloatBuffer;
import java.util.Collection;

/**
 * MainGrass interface for point cloud generators.<br>
 * There are several possibilities to generate a scene graph for jME3.<br>
 * Input can comprise coordinates and colors or just coordinates only.<br>
 * Output is a root node for the jME3 scene graph.<br>
 * <br>
 * Licensing: Use at will and "As is", credits would be nice, but aren't necessary...
 * 
 * @author Ogli
 */
public interface PointCloudJme3GraphGenerator {

	public Node generatePointCloudGraph( float[] pointCoordinates3d ) throws Exception;
	public Node generatePointCloudGraph( Vector3f[] pointCoordinates3d ) throws Exception;
	public Node generatePointCloudGraph( Collection<Vector3f> pointCoordinates3d ) throws Exception;
	public Node generatePointCloudGraph( FloatBuffer pointCoordinates3d ) throws Exception;
	
	public Node generatePointCloudGraph( float[] pointCoordinates3d, float[] colorsRGBA ) throws Exception;
	public Node generatePointCloudGraph( Vector3f[] pointCoordinates3d, ColorRGBA[] colorsRGBA ) throws Exception;
	public Node generatePointCloudGraph( Collection<Vector3f> pointCoordinates3d, Collection<ColorRGBA> colorsRGBA ) throws Exception;
	public Node generatePointCloudGraph( FloatBuffer pointCoordinates3d, FloatBuffer colorsRGBA ) throws Exception;
	
}
