import com.jme3.app.SimpleApplication;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.HeightfieldCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.terrain.Terrain;
import com.jme3.terrain.geomipmap.*;
import com.jme3.terrain.geomipmap.grid.FractalTileLoader;
import com.jme3.terrain.geomipmap.lodcalc.DistanceLodCalculator;
import com.jme3.terrain.noise.ShaderUtils;
import com.jme3.terrain.noise.basis.FilteredBasis;
import com.jme3.terrain.noise.filter.IterativeFilter;
import com.jme3.terrain.noise.filter.OptimizedErode;
import com.jme3.terrain.noise.filter.PerturbFilter;
import com.jme3.terrain.noise.filter.SmoothFilter;
import com.jme3.terrain.noise.fractal.FractalSum;
import com.jme3.terrain.noise.modulator.NoiseModulator;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;

public class TerrainGridTileLoaderTest extends SimpleApplication {
    private CharacterControl player3;
    private FractalSum base;
    private PerturbFilter perturb;
    private OptimizedErode therm;
    private SmoothFilter smooth;
    private IterativeFilter iterate;

    private Material matTerrain;
    private TerrainGrid terrain;
    private float grassScale = 64;
    private float dirtScale = 16;
    private float rockScale = 128;
    private boolean usePhysics = true;
    private boolean physicsAdded = false;

    public static void main(final String[] args) {
        TerrainGridTileLoaderTest app = new TerrainGridTileLoaderTest();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        this.flyCam.setMoveSpeed(100f);
        ScreenshotAppState state = new ScreenshotAppState();
        this.stateManager.attach(state);

        // TERRAIN TEXTURE material
        this.matTerrain = new Material(this.assetManager, "assets/MatDefs/HeightBasedTerrain.j3md");

        // Parameters to material:
        // regionXColorMap: X = 1..4 the texture that should be appliad to state X
        // regionX: a Vector3f containing the following information:
        //      regionX.x: the start height of the region
        //      regionX.y: the end height of the region
        //      regionX.z: the texture scale for the region
        //  it might not be the most elegant way for storing these 3 values, but it packs the data nicely :)
        // slopeColorMap: the texture to be used for cliffs, and steep mountain sites
        // slopeTileFactor: the texture scale for slopes
        // terrainSize: the total size of the terrain (used for scaling the texture)
        // GRASS texture
        Texture grass = this.assetManager.loadTexture("assets/Textures/Terrain/splat/grass.jpg");
        grass.setWrap(WrapMode.Repeat);
        this.matTerrain.setTexture("region1ColorMap", grass);
        this.matTerrain.setVector3("region1", new Vector3f(15, 200, this.grassScale));

        // DIRT texture
        Texture dirt = this.assetManager.loadTexture("assets/Textures/Terrain/splat/dirt.jpg");
        dirt.setWrap(WrapMode.Repeat);
        this.matTerrain.setTexture("region2ColorMap", dirt);
        this.matTerrain.setVector3("region2", new Vector3f(0, 20, this.dirtScale));

        // ROCK texture
        Texture rock = this.assetManager.loadTexture("assets/Textures/Terrain/Rock2/rock.jpg");
        rock.setWrap(WrapMode.Repeat);
        this.matTerrain.setTexture("region3ColorMap", rock);
        this.matTerrain.setVector3("region3", new Vector3f(198, 260, this.rockScale));

        this.matTerrain.setTexture("region4ColorMap", rock);
        this.matTerrain.setVector3("region4", new Vector3f(198, 260, this.rockScale));

        this.matTerrain.setTexture("slopeColorMap", rock);
        this.matTerrain.setFloat("slopeTileFactor", 32);

        this.matTerrain.setFloat("terrainSize", 513);

        this.base = new FractalSum();
        this.base.setRoughness(0.7f);
        this.base.setFrequency(1.0f);
        this.base.setAmplitude(1.0f);
        this.base.setLacunarity(2.12f);
        this.base.setOctaves(8);
        this.base.setScale(0.02125f);
        this.base.addModulator(new NoiseModulator() {

            @Override
            public float value(float... in) {
                return ShaderUtils.clamp(in[0] * 0.5f + 0.5f, 0, 1);
            }
        });

        FilteredBasis ground = new FilteredBasis(this.base);

        this.perturb = new PerturbFilter();
        this.perturb.setMagnitude(0.119f);

        this.therm = new OptimizedErode();
        this.therm.setRadius(5);
        this.therm.setTalus(0.011f);

        this.smooth = new SmoothFilter();
        this.smooth.setRadius(1);
        this.smooth.setEffect(0.7f);

        this.iterate = new IterativeFilter();
        this.iterate.addPreFilter(this.perturb);
        this.iterate.addPostFilter(this.smooth);
        this.iterate.setFilter(this.therm);
        this.iterate.setIterations(1);

        ground.addPreFilter(this.iterate);
        this.terrain = new TerrainGrid("terrain", 33, 129, new FractalTileLoader(ground, 256f));

        this.terrain.setMaterial(this.matTerrain);

        this.terrain.setLocalTranslation(0, 0, 0);
        this.terrain.setLocalScale(2f, 1f, 2f);
//        try {
//            BinaryExporter.getInstance().save(terrain, new File("/Users/normenhansen/Documents/Code/jme3/engine/src/test-data/TerrainGrid/"
//                    + "TerrainGrid.j3o"));
//        } catch (IOException ex) {
//            Logger.getLogger(TerrainFractalGridTest.class.getName()).log(Level.SEVERE, null, ex);
//        }
        
        this.rootNode.attachChild(this.terrain);
        
        TerrainLodControl control = new TerrainGridLodControl(this.terrain, getCamera());
        control.setLodCalculator( new DistanceLodCalculator(65, 2.7f) ); // patch size, and a multiplier
        this.terrain.addControl(control);
        
        final BulletAppState bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);

        this.getCamera().setLocation(new Vector3f(0, 256, 0));
        cam.setRotation(new Quaternion(-0.0075f, 0.949784f, -0.312f, -0.0227f));

        this.viewPort.setBackgroundColor(new ColorRGBA(0.7f, 0.8f, 1f, 1f));

        if (usePhysics) {
            CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(0.5f, 1.8f, 1);
            player3 = new CharacterControl(capsuleShape, 0.5f);
            player3.setJumpSpeed(20);
            player3.setFallSpeed(10);
            player3.setGravity(10);

            player3.setPhysicsLocation(new Vector3f(cam.getLocation().x, 256, cam.getLocation().z));

            bulletAppState.getPhysicsSpace().add(player3);

            terrain.addListener(new TerrainGridListener() {

                @Override
                public void gridMoved(Vector3f newCenter) {
                }

                @Override
                public void tileAttached(Vector3f cell, TerrainQuad quad) {
                    while(quad.getControl(RigidBodyControl.class)!=null){
                        quad.removeControl(RigidBodyControl.class);
                    }
                    quad.addControl(new RigidBodyControl(new HeightfieldCollisionShape(quad.getHeightMap(), terrain.getLocalScale()), 0));
                    bulletAppState.getPhysicsSpace().add(quad);
                }

                @Override
                public void tileDetached(Vector3f cell, TerrainQuad quad) {
                    if (quad.getControl(RigidBodyControl.class) != null) {
                        bulletAppState.getPhysicsSpace().remove(quad);
                        quad.removeControl(RigidBodyControl.class);
                    }
                }

            });
        }
        
        this.initKeys();
    }

    private void initKeys() {
        // You can map one or several inputs to one named action
        this.inputManager.addMapping("Lefts", new KeyTrigger(KeyInput.KEY_A));
        this.inputManager.addMapping("Rights", new KeyTrigger(KeyInput.KEY_D));
        this.inputManager.addMapping("Ups", new KeyTrigger(KeyInput.KEY_W));
        this.inputManager.addMapping("Downs", new KeyTrigger(KeyInput.KEY_S));
        this.inputManager.addMapping("Jumps", new KeyTrigger(KeyInput.KEY_SPACE));
        this.inputManager.addMapping("pick", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        this.inputManager.addListener(this.actionListener, "Lefts");
        this.inputManager.addListener(this.actionListener, "Rights");
        this.inputManager.addListener(this.actionListener, "Ups");
        this.inputManager.addListener(this.actionListener, "Downs");
        this.inputManager.addListener(this.actionListener, "Jumps");
        this.inputManager.addListener(this.actionListener, "pick");
    }
    private boolean left;
    private boolean right;
    private boolean up;
    private boolean down;
    private final ActionListener actionListener = new ActionListener() {

        @Override
        public void onAction(final String name, final boolean keyPressed, final float tpf) {
            if (name.equals("Lefts")) {
                if (keyPressed) {
                    TerrainGridTileLoaderTest.this.left = true;
                } else {
                    TerrainGridTileLoaderTest.this.left = false;
                }
            } else if (name.equals("Rights")) {
                if (keyPressed) {
                    TerrainGridTileLoaderTest.this.right = true;
                } else {
                    TerrainGridTileLoaderTest.this.right = false;
                }
            } else if (name.equals("Ups")) {
                if (keyPressed) {
                    TerrainGridTileLoaderTest.this.up = true;
                } else {
                    TerrainGridTileLoaderTest.this.up = false;
                }
            } else if (name.equals("Downs")) {
                if (keyPressed) {
                    TerrainGridTileLoaderTest.this.down = true;
                } else {
                    TerrainGridTileLoaderTest.this.down = false;
                }
            } else if (name.equals("Jumps")) {
                TerrainGridTileLoaderTest.this.player3.jump();
            } else if (name.equals("pick") && keyPressed) {
                //Terrain picked = terrain.getTerrainAt(player3.getPhysicsLocation());
                Terrain picked = terrain.getTerrainAtCell(terrain.getCurrentCell());
                System.out.println("** cell "+player3.getPhysicsLocation()+" picked terrain: "+picked);
            }
        }
    };
    private final Vector3f walkDirection = new Vector3f();

    @Override
    public void simpleUpdate(final float tpf) {
        Vector3f camDir = this.cam.getDirection().clone().multLocal(0.6f);
        Vector3f camLeft = this.cam.getLeft().clone().multLocal(0.4f);
        this.walkDirection.set(0, 0, 0);
        if (this.left) {
            this.walkDirection.addLocal(camLeft);
        }
        if (this.right) {
            this.walkDirection.addLocal(camLeft.negate());
        }
        if (this.up) {
            this.walkDirection.addLocal(camDir);
        }
        if (this.down) {
            this.walkDirection.addLocal(camDir.negate());
        }

        if (usePhysics) {
            this.player3.setWalkDirection(this.walkDirection);
            this.cam.setLocation(this.player3.getPhysicsLocation());
        }
    }
}
