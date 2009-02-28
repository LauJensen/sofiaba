(in-ns 'dk.bestinclass.sofiaba)

(import '(com.jme.scene       Skybox$Face Spatial$LightCombineMode)
        '(com.jme.image       Texture$ApplyMode Texture$CombinerFunctionRGB
                              Texture$CombinerSource Texture$CombinerOperandRGB
                              Texture$CombinerScale Texture$WrapMode )
        '(com.jme.scene.state BlendState BlendState$SourceFunction
                              BlendState$DestinationFunction BlendState$TestFunction)
        '(com.jme.animation   SpatialTransformer)
        '(com.jme.math        Quaternion FastMath)
        '(com.jmex.terrain    TerrainPage))

(def BilNoMipMap com.jme.image.Texture$MinificationFilter/BilinearNoMipMaps)
(def Bil         com.jme.image.Texture$MagnificationFilter/Bilinear)

;========= GLOBALS

(def *globals* (ref (hash-map)))

(defn $get
  " Retrieves the value in keyword k - Global vars "
  [k]
  (@*globals* k))

(defn $set
  " Set the keyword k to a value - Global vars "
  [k value]
  (dosync
   (alter *globals* assoc k value)))

(defn indoctrinate
  " Takes a map of global definitions and stores them using $set "
  [& args]
  (let [map-of-candidates (apply hash-map args)]
    (doseq [this-key (keys map-of-candidates)]
      ($set this-key (this-key map-of-candidates)))))

(defn addToRoot
  " Helper for adding multiple objects to the root node "
  [& children]
  (if-let [rootNode  ($get :rootnode)]
    (doseq [child children]
      (.attachChild rootNode child))
    (throw (Exception. "Global key :rootnode must be set before calling"))))

;======= INPUT HANDLING

(defn makeInputHandler
  " Spawns a new Inputhandler from a given camera, input handler is returned "
  [camera keyboardspeed mousespeed]
  (let [ input       (FirstPersonHandler. camera) ]
    (.setActionSpeed (.getKeyboardLookHandler input)  keyboardspeed)
    (.setActionSpeed (.getMouseLookHandler    input)  mousespeed)
    input))
  
(defn attachCommands
  " Takes a hash-map of commands as keys and KEYS as the values, ex.
     (attachCommands {:exit KeyInput/KEY_ESCAPE}) "
  [map-of-commands]
  (doseq [key (keys map-of-commands)]
     (.. KeyBindingManager (getKeyBindingManager) (set (name key) (key map-of-commands)))))

 ;======= LIGHTS

(defmacro mapmac
  [t f c]
  (lazy-cons t (map (eval f) c)))   ;; TODO: UPDATE TO LATEST REV OF CLOJURE

(defmacro >RGBA
  [vec]
  `(mapmac ColorRGBA. (fn [x#] (float x#)) ~vec))

(defmacro >Vec3f
  [vec]
  `(mapmac Vector3f. (fn [x#] (float x#)) ~vec))

(defn makeDirectedLight
  " Returns a LightState containing only a DirectionalLight "
  [diffuse ambient direction]
  (if-let [ display ($get :display) ]
    (let [lightState    (.. display (getRenderer) (createLightState))
          light         (DirectionalLight.) ]
      (doto light
        (.setDiffuse     (eval `(>RGBA  ~diffuse)))
        (.setAmbient     (eval `(>RGBA  ~ambient)))
        (.setDirection   (eval `(>Vec3f ~direction)))
        (.setEnabled     true))
      (doto lightState
        .detachAll
        (.attach         light)
        (.setEnabled     true)))
    (throw (Exception. "Global key :display must be set before calling"))))

 ;======= CAMERA

(defn makeCamera
  " Makes a camera for DISPLAYs renderer, sets the global :camera and returns the camera object "
  [display screen near far]
  ($set :camera (.. display (getRenderer) (createCamera (:width  screen) (:height screen))))
  (let [ cam    ($get :camera)
         loc    (Vector3f.  500     150   1000)
         left   (Vector3f.   -1.0     0      0)
         up     (Vector3f.    0       1      0)
         dir    (Vector3f.    0       0     -1)
         ratio  (float (/ (:width screen) (:height screen))) ]
    (.setFrustum            cam near far     50 50 50 50)
    (.setFrustumPerspective cam 45   ratio near far)
    (.setFrame              cam loc left up dir)
    (.update                cam)
    cam))

 ;======= ZBuffer

(defn makeZBuffer
  [func]
  (if-let [display  ($get :display)]
    (let  [zBuf     (.. display (getRenderer) (createZBufferState))]
      (doto zBuf
        (.setEnabled true)
        (.setFunction func)))
    (throw (Exception. "Global key :display must be set before calling"))))

;======= RESOURCES: STOP - HELPERS START

(defn >Image
  " A helper to convert a file to an Image, via ImageIcon "
  [resource-key]
  (if (keyword? resource-key)
    (.getImage (ImageIcon. (get-resource resource-key)))
    (.getImage (.getImageIcon resource-key)))) ; Presumed ProceduralTextureGenerator

(defn flipQuad
  [s]
  (let [rotQuad   (Quaternion.) ]
    (.fromAngleNormalAxis rotQuad (* FastMath/PI (float 0.5)) Vector3f/UNIT_X)
    (.setLocalRotation s rotQuad)))

(defn setTexture
  [obj display tex]
  (let [texState  (.. display (getRenderer) (createTextureState))
        texture   (TextureManager/loadTexture (>Image tex) BilNoMipMap Bil true) ]
    (.setTexture texState texture 0)
    (.setRenderState obj texState)))

