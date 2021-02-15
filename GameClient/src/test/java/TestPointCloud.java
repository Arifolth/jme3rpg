import clouds.AbstractPointCloudGraphGenerator;
import clouds.RawPointCloudGraphGenerator;
import com.jme3.app.SimpleApplication;
import com.jme3.math.Vector3f;

import java.util.Random;


/**
 * A simple test application for jME3 point cloud generators.<br>
 * Generates some random points, and optionally some random colors.<br>
 * These are then passed as input into an implementation of the generator interface.<br>
 * The generator outputs a Node, which is attached to the scene root.<br>
 * Tests on my 2009 desktop machine showed, that more than 2 million points can be rendered at max framerate.<br>
 * <br>
 * Licensing: Use at will and "As is", credits would be nice, but aren't necessary...
 * 
 * @author Ogli
 */
public class TestPointCloud extends SimpleApplication{

	public Vector3f MIN = new Vector3f(-4.675392f, -3.287754f, 0.0f);
	public Vector3f MAX = new Vector3f(5.847956f, 2.126556f, 19.251f);
	public Vector3f CENTER = MAX.add(MIN).mult(0.5f);
	public Vector3f DELTA = MAX.subtract(CENTER);
	public int NUMPOINTS = 2000000;
	
	public static void main(String[] args) {
		TestPointCloud test1 = new TestPointCloud();
		test1.start();
	}

	@Override
	public void simpleInitApp() {

		float[] points = generatePoints(NUMPOINTS);
		float[] colors = generateColors(NUMPOINTS);
		
		AbstractPointCloudGraphGenerator generator;
		
		generator = new RawPointCloudGraphGenerator(assetManager);
		//generator = new SimplePointCloudGraphGenerator(assetManager); //<-- not ready yet...
		//generator = new LodPointCloudGraphGenerator(assetManager); //<-- not ready yet...
		
		try
		{
			rootNode.attachChild(generator.generatePointCloudGraph(points)); //<-- all points white
			//rootNode.attachChild(generator.generatePointCloudGraph(points,colors)); //<-- random point colors
		}
		catch(Exception e)
		{
			this.handleError(e.getMessage(), e);
		}

		cam.setFrustumPerspective(45.0f, ((float)cam.getWidth()) / ((float)cam.getHeight()), 0.05f, 100.0f);
		
		cam.setLocation(new Vector3f(0, 0, -5));
		cam.lookAtDirection(Vector3f.UNIT_Z, Vector3f.UNIT_Y);
		
		cam.update();
	}

	public float[] generatePoints(int numberOfPoints)
	{
		float[] result = new float[3*numberOfPoints];
		Random random = new Random();
		for(int i = 0; i < numberOfPoints; i++)
		{
			result[i*3]     = CENTER.x + DELTA.x*(random.nextFloat() - random.nextFloat());
			result[i*3 + 1] = CENTER.y + DELTA.y*(random.nextFloat() - random.nextFloat());
			result[i*3 + 2] = CENTER.z + DELTA.z*(random.nextFloat() - random.nextFloat());
		}
		return result;
	}

	public float[] generateColors(int numberOfPoints)
	{
		float[] result = new float[4*numberOfPoints];
		Random random = new Random();
		for(int i = 0; i < numberOfPoints; i++)
		{
			result[i*4]     = random.nextFloat();
			result[i*4 + 1] = 0.5f + 0.5f*random.nextFloat();
			result[i*4 + 2] = random.nextFloat();
			result[i*4 + 3] = 1.0f;
		}
		return result;
	}
}
